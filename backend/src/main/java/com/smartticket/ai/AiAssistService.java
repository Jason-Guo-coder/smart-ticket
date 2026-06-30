package com.smartticket.ai;

import com.smartticket.ai.client.AiHttpClient;
import com.smartticket.ai.model.AiSuggestion;
import com.smartticket.ai.model.SimilarTicket;
import com.smartticket.ai.parser.AiResultParser;
import com.smartticket.ai.prompt.PromptBuilder;
import com.smartticket.common.util.RedisUtil;
import com.smartticket.ticket.enums.Category;
import com.smartticket.ticket.enums.Priority;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.user.mapper.EngineerProfileMapper;
import com.smartticket.user.vo.EngineerVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 辅助分析的【唯一对外入口】（ARCHITECTURE §6 / B11）。
 * 流程：缓存 → 调模型(超时) → 解析(失败重试1次) → 相似工单 → 建议工程师 → 命中写缓存。
 * 任一环节失败 → 返回降级建议（degraded），绝不抛异常打断主流程（B5）。
 */
@Slf4j
@Service
public class AiAssistService {

    private static final int SIMILAR_LIMIT = 3;

    private final PromptBuilder promptBuilder;
    private final AiHttpClient aiHttpClient;
    private final AiResultParser parser;
    private final RedisUtil redisUtil;
    private final TicketMapper ticketMapper;
    private final EngineerProfileMapper engineerMapper;
    private final long cacheTtlMinutes;

    public AiAssistService(PromptBuilder promptBuilder, AiHttpClient aiHttpClient,
                           AiResultParser parser, RedisUtil redisUtil,
                           TicketMapper ticketMapper, EngineerProfileMapper engineerMapper,
                           @org.springframework.beans.factory.annotation.Value(
                                   "${smart-ticket.ai.cache-ttl-minutes}") long cacheTtlMinutes) {
        this.promptBuilder = promptBuilder;
        this.aiHttpClient = aiHttpClient;
        this.parser = parser;
        this.redisUtil = redisUtil;
        this.ticketMapper = ticketMapper;
        this.engineerMapper = engineerMapper;
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public AiSuggestion analyze(String title, String content) {
        long start = System.currentTimeMillis();
        String cacheKey = "ai:result:" + md5(title + "|" + content);

        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof AiSuggestion s) {
            s.setElapsedMs(System.currentTimeMillis() - start);
            return s;
        }

        AiResultParser.Parsed parsed = callWithRetry(title, content);

        AiSuggestion suggestion = new AiSuggestion();
        if (parsed == null) {
            suggestion.setDegraded(true);
        } else {
            Category cat = parsed.category();
            Priority pri = parsed.priority();
            suggestion.setCategory(cat.name());
            suggestion.setCategoryLabel(cat.getLabel());
            suggestion.setPriority(pri.name());
            suggestion.setPriorityLabel(pri.getLabel());
            // 建议工程师：按类别技能 + 最低负载（数据驱动，比让模型点名更可靠）
            EngineerVO eng = engineerMapper.selectBestByCategory(cat.name());
            if (eng != null) {
                suggestion.setSuggestedEngineerId(eng.getUserId());
                suggestion.setSuggestedEngineerName(eng.getRealName());
            }
        }

        // 相似工单：有类别时按类别（强信号）；降级无类别时退化为标题关键词 LIKE
        String category = parsed == null ? null : parsed.category().name();
        String kw = parsed == null ? keyword(title) : null;
        List<SimilarTicket> similar = ticketMapper.selectSimilar(category, kw, SIMILAR_LIMIT);
        suggestion.setSimilarTickets(similar);

        if (!suggestion.isDegraded()) {
            redisUtil.set(cacheKey, suggestion, cacheTtlMinutes, TimeUnit.MINUTES);
        }
        suggestion.setElapsedMs(System.currentTimeMillis() - start);
        return suggestion;
    }

    private AiResultParser.Parsed callWithRetry(String title, String content) {
        if (!aiHttpClient.available()) {
            return null;
        }
        String prompt = promptBuilder.build(title, content);
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                AiResultParser.Parsed p = parser.parse(aiHttpClient.complete(prompt));
                if (p != null) {
                    return p;
                }
                log.warn("AI 输出解析失败，第 {} 次", attempt);
            } catch (Exception e) {
                log.warn("AI 调用失败（第 {} 次）: {}", attempt, e.getMessage());
            }
        }
        return null;
    }

    /** 取标题前若干字作关键词（MVP 简化）。 */
    private String keyword(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String t = title.trim();
        return t.length() > 6 ? t.substring(0, 6) : t;
    }

    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
