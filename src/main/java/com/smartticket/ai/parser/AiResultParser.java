package com.smartticket.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.ticket.enums.Category;
import com.smartticket.ticket.enums.Priority;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 解析并校验 AI 输出的 JSON（B5）。无法解析/枚举非法返回 null，由上层重试/降级。 */
@Component
public class AiResultParser {

    private static final Pattern JSON = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);
    private final ObjectMapper om = new ObjectMapper();

    public record Parsed(Category category, Priority priority) {
    }

    public Parsed parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher m = JSON.matcher(text);
        if (!m.find()) {
            return null;
        }
        try {
            JsonNode node = om.readTree(m.group());
            Category category = toEnum(Category.class, node.path("category").asText(null));
            Priority priority = toEnum(Priority.class, node.path("priority").asText(null));
            if (category == null || priority == null) {
                return null;
            }
            return new Parsed(category, priority);
        } catch (Exception e) {
            return null;
        }
    }

    private <E extends Enum<E>> E toEnum(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
