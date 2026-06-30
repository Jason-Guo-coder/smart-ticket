package com.smartticket.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 大模型 HTTP 调用（ARCHITECTURE §6，Anthropic Messages API 风格）。
 * 设超时；未配置 api-key 或异常时抛出，由 AiAssistService 统一降级。
 */
@Slf4j
@Component
public class AiHttpClient {

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutMs;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http;

    public AiHttpClient(@Value("${smart-ticket.ai.enabled}") boolean enabled,
                        @Value("${smart-ticket.ai.base-url}") String baseUrl,
                        @Value("${smart-ticket.ai.api-key}") String apiKey,
                        @Value("${smart-ticket.ai.model}") String model,
                        @Value("${smart-ticket.ai.timeout-ms}") int timeoutMs) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public boolean available() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** 调用模型，返回回复文本；不可用或失败抛异常。 */
    public String complete(String prompt) throws Exception {
        if (!available()) {
            throw new IllegalStateException("AI 未启用或未配置 api-key");
        }
        // OpenAI 兼容格式（DeepSeek /chat/completions）
        ObjectNode body = om.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 256);
        body.put("temperature", 0);
        body.put("stream", false);
        body.putObject("response_format").put("type", "json_object");
        ArrayNode messages = body.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("content-type", "application/json")
                .header("authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("AI 接口返回 " + resp.statusCode());
        }
        JsonNode root = om.readTree(resp.body());
        // OpenAI/DeepSeek: { choices: [ { message: { content: ... } } ] }
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText("");
        }
        return root.toString();
    }
}
