package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.runtime.AiChatService;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Chat 节点执行器
 * <p>
 * 在工作流中执行 LLM 对话调用，支持模型选择和系统提示词配置
 * <p>
 * 节点配置示例：
 * {
 *   "modelId": 1,
 *   "systemPrompt": "You are a Goat Cloud assistant."
 * }
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNodeExecutor implements NodeExecutor {

    private final AiChatService chatService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "CHAT";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            if (nodeConfig != null && !nodeConfig.isBlank()) {
                config = objectMapper.readValue(nodeConfig, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            }

            // Resolve modelId from config, fallback to context
            Long modelId = toLong(config.get("modelId"));
            if (modelId == null) {
                modelId = toLong(context.get("modelId"));
            }

            // Resolve systemPrompt from config, default "You are a Goat Cloud assistant."
            String systemPrompt = asString(config.get("systemPrompt"));
            if (systemPrompt.isBlank()) {
                systemPrompt = "You are a Goat Cloud assistant.";
            }

            // Build chat request
            AiChatRequest chatRequest = new AiChatRequest();
            chatRequest.setMessage(asString(context.get("userMessage")));
            chatRequest.setConversationId(asString(context.get("conversationId")));
            chatRequest.setModelId(modelId);
            chatRequest.setSystemPrompt(systemPrompt);

            AiChatResponse chatResponse = chatService.chat(chatRequest);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("content", chatResponse.getMessage() == null ? null : chatResponse.getMessage().getContent());
            output.put("usage", chatResponse.getUsage());
            output.put("mock", chatResponse.getMock());

            context.put("chatResult", output);

            return NodeResult.ok(objectMapper.writeValueAsString(output));

        } catch (Exception e) {
            log.error("Chat node execution error", e);
            return NodeResult.fail("Chat execution failed: " + e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }
}