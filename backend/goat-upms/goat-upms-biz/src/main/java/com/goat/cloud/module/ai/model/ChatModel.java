package com.goat.cloud.module.ai.model;

import java.util.List;
import java.util.function.Consumer;

/**
 * Chat 模型接口
 * 支持同步和流式调用
 */
public interface ChatModel extends Model {

    /**
     * 同步聊天
     */
    String chat(ChatRequest request);

    /**
     * 流式聊天
     */
    void chatStream(StreamChatRequest request);

    /**
     * 聊天请求
     */
    record ChatRequest(
            String systemPrompt,
            String userMessage,
            List<ChatMessage> history,
            Double temperature,
            Integer maxTokens
    ) {}

    /**
     * 流式聊天请求
     */
    record StreamChatRequest(
            String systemPrompt,
            String userMessage,
            List<ChatMessage> history,
            Double temperature,
            Integer maxTokens,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {}

    /**
     * 聊天消息
     */
    record ChatMessage(String role, String content) {}
}
