package com.smartticket.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * AI 智能分析结果（PRD §4.2 契约）。
 * degraded=true 表示 AI 降级（无建议），主流程不受影响（B5）。
 */
@Data
public class AiSuggestion implements Serializable {

    private String category;          // 枚举名，可空
    private String categoryLabel;     // 中文
    private String priority;          // HIGH/MID/LOW，可空
    private String priorityLabel;
    private Long suggestedEngineerId;
    private String suggestedEngineerName;
    private List<SimilarTicket> similarTickets = Collections.emptyList();
    private boolean degraded;
    private long elapsedMs;

    public static AiSuggestion degraded() {
        AiSuggestion s = new AiSuggestion();
        s.setDegraded(true);
        return s;
    }
}
