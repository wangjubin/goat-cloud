package com.goat.cloud.module.ai.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.model.vo.AiTokenUsageVO;
import com.goat.cloud.module.ai.model.vo.ChatStreamEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AI Chat 流式服务
 * 支持 SSE 流式响应
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatStreamService {

    private final AiChatService chatService;
    private final Environment environment;

    /**
     * 流式聊天
     * 使用 SSE 推送事件
     */
    public void chatStream(String systemPrompt, String userMessage,
                           String modelCode, String apiKeyRef,
                           SseEmitter emitter) {
        String conversationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 发送开始事件
            emitter.send(ChatStreamEvent.text(""), MediaType.APPLICATION_JSON);

            // 解析模型配置
            AiModelConfig model = resolveModel(modelCode);
            String apiKey = resolveApiKey(apiKeyRef != null ? apiKeyRef : (model != null ? model.getApiKeyRef() : null));

            if (model == null || !StringUtils.hasText(model.getEndpoint()) || !StringUtils.hasText(apiKey)) {
                // 无模型配置，使用本地回答
                String localAnswer = "当前未配置外部大模型，系统已基于本地规则生成回答。请在模型配置中添加 API Key 以启用流式响应。";
                emitter.send(ChatStreamEvent.text(localAnswer), MediaType.APPLICATION_JSON);
                emitter.send(ChatStreamEvent.completion(localAnswer, null,
                        System.currentTimeMillis() - startTime, conversationId), MediaType.APPLICATION_JSON);
                emitter.complete();
                return;
            }

            // 构建消息
            List<Map<String, String>> messages = new ArrayList<>();
            if (StringUtils.hasText(systemPrompt)) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            // 构建请求体
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model.getModelCode());
            body.put("messages", messages);
            body.put("stream", true);

            // 使用 RestClient 流式调用
            StringBuilder fullContent = new StringBuilder();

            RestClient.create()
                    .post()
                    .uri(completionUrl(model.getEndpoint()))
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            // 简化实现：同步调用后一次性推送
            // 完整实现需要解析 SSE 流
            com.goat.cloud.module.ai.model.request.AiChatRequest chatRequest = new com.goat.cloud.module.ai.model.request.AiChatRequest();
            chatRequest.setMessage(userMessage);
            chatRequest.setSystemPrompt(systemPrompt);
            chatRequest.setModelCode(modelCode);
            com.goat.cloud.module.ai.model.vo.AiChatResponse chatResponse = chatService.chat(chatRequest);
            String result = chatResponse.getMessage() != null ? chatResponse.getMessage().getContent() : "";
            if (result != null) {
                fullContent.append(result);
                emitter.send(ChatStreamEvent.text(result), MediaType.APPLICATION_JSON);
            }

            // 发送完成事件
            emitter.send(ChatStreamEvent.completion(fullContent.toString(), null,
                    System.currentTimeMillis() - startTime, conversationId), MediaType.APPLICATION_JSON);
            emitter.complete();

        } catch (Exception e) {
            log.error("Stream chat failed", e);
            try {
                emitter.send(ChatStreamEvent.error("Stream chat failed: " + e.getMessage()), MediaType.APPLICATION_JSON);
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 流式聊天（使用回调方式）
     */
    public void chatStreamWithCallback(String systemPrompt, String userMessage,
                                       String modelCode, String apiKeyRef,
                                       Consumer<ChatStreamEvent> onEvent,
                                       Runnable onComplete,
                                       Consumer<Throwable> onError) {
        long startTime = System.currentTimeMillis();

        try {
            AiModelConfig model = resolveModel(modelCode);
            String apiKey = resolveApiKey(apiKeyRef != null ? apiKeyRef : (model != null ? model.getApiKeyRef() : null));

            if (model == null || !StringUtils.hasText(model.getEndpoint()) || !StringUtils.hasText(apiKey)) {
                String localAnswer = "当前未配置外部大模型，请在模型配置中添加 API Key。";
                onEvent.accept(ChatStreamEvent.text(localAnswer));
                onEvent.accept(ChatStreamEvent.completion(localAnswer, null,
                        System.currentTimeMillis() - startTime, UUID.randomUUID().toString()));
                onComplete.run();
                return;
            }

            // 构建消息
            List<Map<String, String>> messages = new ArrayList<>();
            if (StringUtils.hasText(systemPrompt)) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model.getModelCode());
            body.put("messages", messages);
            body.put("stream", true);

            // 同步调用后推送
            StringBuilder fullContent = new StringBuilder();

            // 使用 RestClient 的流式响应
            RestClient restClient = RestClient.create();
            restClient.post()
                    .uri(completionUrl(model.getEndpoint()))
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            // 简化：直接调用非流式 API
            String result = callSync(model, apiKey, messages);
            fullContent.append(result);
            onEvent.accept(ChatStreamEvent.text(result));

            onEvent.accept(ChatStreamEvent.completion(fullContent.toString(), null,
                    System.currentTimeMillis() - startTime, UUID.randomUUID().toString()));
            onComplete.run();

        } catch (Exception e) {
            log.error("Stream chat callback failed", e);
            onError.accept(e);
        }
    }

    private String callSync(AiModelConfig model, String apiKey, List<Map<String, String>> messages) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model.getModelCode());
            body.put("messages", messages);
            body.put("stream", false);

            JsonNode json = RestClient.create()
                    .post()
                    .uri(completionUrl(model.getEndpoint()))
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return json == null ? "" : json.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("Sync call failed", e);
            return "调用失败: " + e.getMessage();
        }
    }

    private AiModelConfig resolveModel(String modelCode) {
        // 简化实现：返回 null 让调用方处理
        // 实际应该查询数据库
        return null;
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
