package com.goat.cloud.module.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 智能体对话责任链执行器
 * 按 order 排序执行所有 handler，任一 handler 终止则短路
 */
@Slf4j
@Component
public class AgentChatChain {

    private final List<AgentChatHandler> handlers;

    public AgentChatChain(List<AgentChatHandler> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(AgentChatHandler::order))
                .toList();
    }

    /**
     * 执行 handler 链
     */
    public AgentChatContext execute(AgentChatContext ctx) {
        for (AgentChatHandler handler : handlers) {
            if (ctx.isTerminated()) {
                log.debug("Chain terminated at {}, reason: {}", handler.getClass().getSimpleName(), ctx.getErrorMessage());
                break;
            }
            try {
                handler.handle(ctx);
            } catch (Exception e) {
                log.error("Handler {} failed", handler.getClass().getSimpleName(), e);
                ctx.terminate("Handler failed: " + e.getMessage());
            }
        }
        return ctx;
    }
}
