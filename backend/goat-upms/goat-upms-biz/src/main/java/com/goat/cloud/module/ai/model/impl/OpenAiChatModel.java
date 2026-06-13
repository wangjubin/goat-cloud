package com.goat.cloud.module.ai.model.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.model.ChatModel;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 ChatModel 实现
 * 支持 OpenAI、DeepSeek、通义千问等兼容 API
 */
@Slf4j
@Component
public class OpenAiChatModel implements ChatModel {

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private AiModelConfig config;

    public OpenAiChatModel(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    public boolean supports(String modelKey) {
        // 通用 OpenAI 兼容模型，支持所有模型
        return true;
    }

    @Override
    public void setModelConfig(AiModelConfig config) {
        this.config = config;
    }

    @Override
    public AiModelConfig getModelConfig() {
        return this.config;
    }

    @Override
    public String chat(ChatRequest request) {
        if (config == null) {
            throw new IllegalStateException("Model config not set");
        }

        String apiKey = resolveApiKey(config.getApiKeyRef());
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("API key not resolved for model: " + config.getModelCode());
        }

        try {
            List<Map<String, String>> messages = buildMessages(request);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModelCode());
            body.put("messages", messages);
            body.put("stream", false);
            if (request.temperature() != null) {
                body.put("temperature", request.temperature());
            }
            if (request.maxTokens() != null) {
                body.put("max_tokens", request.maxTokens());
            }

            JsonNode json = RestClient.create()
                    .post()
                    .uri(completionUrl(config.getEndpoint()))
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = json == null ? null
                    : json.path("choices").path(0).path("message").path("content").asText(null);

            if (!StringUtils.hasText(content)) {
                throw new RuntimeException("Provider returned no assistant content");
            }

            return content;
        } catch (Exception e) {
            log.error("Chat model call failed: {}", config.getModelCode(), e);
            throw new RuntimeException("Chat model call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(StreamChatRequest request) {
        if (config == null) {
            request.onError().accept(new IllegalStateException("Model config not set"));
            return;
        }

        String apiKey = resolveApiKey(config.getApiKeyRef());
        if (!StringUtils.hasText(apiKey)) {
            request.onError().accept(new IllegalStateException("API key not resolved"));
            return;
        }

        try {
            List<Map<String, String>> messages = buildMessages(request);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModelCode());
            body.put("messages", messages);
            body.put("stream", true);
            if (request.temperature() != null) {
                body.put("temperature", request.temperature());
            }
            if (request.maxTokens() != null) {
                body.put("max_tokens", request.maxTokens());
            }

            // 使用 RestClient 的流式响应
            RestClient.create()
                    .post()
                    .uri(completionUrl(config.getEndpoint()))
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            // 简化实现：同步调用后一次性返回
            // 完整实现需要使用 SSE 解析
            String result = chat(new ChatRequest(
                    request.systemPrompt(), request.userMessage(),
                    request.history(), request.temperature(), request.maxTokens()));
            request.onToken().accept(result);
            request.onComplete().run();
        } catch (Exception e) {
            log.error("Stream chat model call failed: {}", config.getModelCode(), e);
            request.onError().accept(e);
        }
    }

    private List<Map<String, String>> buildMessages(ChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        if (StringUtils.hasText(request.systemPrompt())) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }

        if (request.history() != null) {
            for (ChatMessage msg : request.history()) {
                if (StringUtils.hasText(msg.role()) && StringUtils.hasText(msg.content())) {
                    messages.add(Map.of("role", msg.role(), "content", msg.content()));
                }
            }
        }

        if (StringUtils.hasText(request.userMessage())) {
            messages.add(Map.of("role", "user", "content", request.userMessage()));
        }

        return messages;
    }

    private List<Map<String, String>> buildMessages(StreamChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        if (StringUtils.hasText(request.systemPrompt())) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }

        if (request.history() != null) {
            for (ChatMessage msg : request.history()) {
                if (StringUtils.hasText(msg.role()) && StringUtils.hasText(msg.content())) {
                    messages.add(Map.of("role", msg.role(), "content", msg.content()));
                }
            }
        }

        if (StringUtils.hasText(request.userMessage())) {
            messages.add(Map.of("role", "user", "content", request.userMessage()));
        }

        return messages;
    }

    private String resolveApiKey(String apiKeyRef) {
        if (!StringUtils.hasText(apiKeyRef)) return null;
        String ref = apiKeyRef.trim();
        if (ref.startsWith("ENV:")) return lookupSecret(ref.substring(4));
        if (ref.startsWith("PROP:")) return environment.getProperty(ref.substring(5));
        if (ref.startsWith("${") && ref.endsWith("}")) return lookupSecret(ref.substring(2, ref.length() - 1));
        if (ref.startsWith("VALUE:")) return ref.substring(6);
        return lookupSecret(ref);
    }

    private String lookupSecret(String name) {
        if (!StringUtils.hasText(name)) return null;
        String value = environment.getProperty(name);
        if (!StringUtils.hasText(value)) value = System.getenv(name);
        return StringUtils.hasText(value) ? value : null;
    }

    private String completionUrl(String endpoint) {
        String trimmed = endpoint.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.endsWith("/chat/completions")) return trimmed;
        if (trimmed.endsWith("/v1")) return trimmed + "/chat/completions";
        return trimmed + "/v1/chat/completions";
    }
}
