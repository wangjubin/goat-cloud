package com.goat.cloud.module.ai.agent.handler;

import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.memory.ShortTermMessage;
import com.goat.cloud.module.ai.memory.ShortTermMemoryStore;
import com.goat.cloud.module.ai.service.AiConversationService;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 会话管理：创建/复用会话，保存用户消息，加载历史
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationHandler implements AgentChatHandler {

    private final AiConversationService conversationService;
    private final ShortTermMemoryStore shortTermMemoryStore;

    private static final int DEFAULT_SHORT_TERM_WINDOW = 20;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) return;

        int windowSize = AiRuntimeHelper.toInteger(ctx.getOptions().get("shortTermWindow"), DEFAULT_SHORT_TERM_WINDOW);

        // 加载短期记忆历史
        List<ShortTermMessage> history = shortTermMemoryStore.loadHistory(ctx.getConversationId(), windowSize);
        ctx.setHistoryMessages(history);

        // 保存用户消息到短期记忆
        if (StringUtils.hasText(ctx.getContent())) {
            shortTermMemoryStore.append(ctx.getConversationId(), "user", ctx.getContent(), windowSize);

            // 持久化到数据库
            try {
                conversationService.getOrCreateConversation(
                        ctx.getConversationId(), ctx.getAgentId(), ctx.getUserId(), ctx.getContent());
                conversationService.saveMessage(
                        ctx.getConversationId(), ctx.getAgentId(), ctx.getUserId(),
                        "user", ctx.getContent(), null);
            } catch (Exception ex) {
                log.warn("Conversation persistence failed", ex);
            }
        }

        log.debug("ConversationHandler: historySize={}, conversationId={}",
                history.size(), ctx.getConversationId());
    }

    @Override
    public int order() {
        return 20;
    }
}
