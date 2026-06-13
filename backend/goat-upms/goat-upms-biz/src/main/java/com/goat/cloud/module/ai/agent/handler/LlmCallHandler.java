package com.goat.cloud.module.ai.agent.handler;

import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.memory.ShortTermMessage;
import com.goat.cloud.module.ai.memory.ShortTermMemoryStore;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.runtime.AiChatService;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import com.goat.cloud.module.ai.runtime.model.RagSearchHit;
import com.goat.cloud.module.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM 调用：组装最终 prompt，调用 AiChatService，保存助手回复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmCallHandler implements AgentChatHandler {

    private final AiChatService chatService;
    private final AiConversationService conversationService;
    private final ShortTermMemoryStore shortTermMemoryStore;

    private static final int DEFAULT_SHORT_TERM_WINDOW = 20;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) return;

        // 构建最终的 system prompt（注入历史上下文）
        String finalPrompt = buildFinalPrompt(ctx);

        // 构建聊天请求
        AiChatRequest chatRequest = new AiChatRequest();
        chatRequest.setConversationId(ctx.getConversationId());
        chatRequest.setModelId(ctx.getAgent().getModelId());
        chatRequest.setMessage(ctx.getContent());
        chatRequest.setSystemPrompt(finalPrompt);

        // 传递选项
        ctx.getOptions().put("bizType", "AGENT");
        ctx.getOptions().put("agentId", ctx.getAgentId());
        ctx.getOptions().put("rag", Boolean.FALSE);
        chatRequest.setOptions(ctx.getOptions());

        // 调用 LLM
        AiChatResponse chat = chatService.chat(chatRequest);

        // 保存助手回复
        if (chat.getMessage() != null && StringUtils.hasText(chat.getMessage().getContent())) {
            int windowSize = AiRuntimeHelper.toInteger(ctx.getOptions().get("shortTermWindow"), DEFAULT_SHORT_TERM_WINDOW);
            shortTermMemoryStore.append(ctx.getConversationId(), "assistant", chat.getMessage().getContent(), windowSize);
            try {
                conversationService.saveMessage(ctx.getConversationId(), ctx.getAgentId(), ctx.getUserId(),
                        "assistant", chat.getMessage().getContent(), null);
            } catch (Exception ex) {
                log.warn("Assistant message persistence failed", ex);
            }
        }

        // 将 chat 响应存入 metadata 供上层使用
        ctx.getRuntimeMetadata().put("chatResponse", chat);
        ctx.getRuntimeMetadata().put("finishReason", chat.getFinishReason());
        ctx.getRuntimeMetadata().put("mock", chat.getMock());

        log.debug("LlmCallHandler: finishReason={}, mock={}", chat.getFinishReason(), chat.getMock());
    }

    private String buildFinalPrompt(AgentChatContext ctx) {
        StringBuilder prompt = new StringBuilder(ctx.getSystemPrompt());

        // 注入历史消息上下文
        if (!ctx.getHistoryMessages().isEmpty()) {
            prompt.append("\n\n## Conversation History\n\n");
            for (ShortTermMessage msg : ctx.getHistoryMessages()) {
                prompt.append("**").append(msg.getRole()).append("**: ").append(msg.getContent()).append("\n\n");
            }
        }

        // 注入工具结果
        if (!ctx.getToolResults().isEmpty()) {
            prompt.append("\n\n## Tool Results\n\n");
            for (Map<String, Object> result : ctx.getToolResults()) {
                prompt.append("- ").append(result.get("kind")).append(": ").append(result.get("status")).append("\n");
            }
        }

        prompt.append("\n\nAnswer requirements: cite retrieved chunks when useful, mention skipped/simulated tools clearly.");
        return prompt.toString();
    }

    @Override
    public int order() {
        return 80;
    }
}
