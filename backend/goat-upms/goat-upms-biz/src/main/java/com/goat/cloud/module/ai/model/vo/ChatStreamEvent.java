package com.goat.cloud.module.ai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 流式消息事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamEvent {

    /**
     * 事件类型
     * - thinking: 思考过程
     * - text: 文本内容
     * - completion: 完成
     * - error: 错误
     */
    private String type;

    /**
     * 事件内容
     */
    private String content;

    /**
     * 完整文本（仅 completion 事件）
     */
    private String fullText;

    /**
     * 思考过程完整文本（仅 completion 事件）
     */
    private String fullThinking;

    /**
     * 耗时毫秒（仅 completion 事件）
     */
    private Long durationMs;

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 创建思考事件
     */
    public static ChatStreamEvent thinking(String content) {
        return new ChatStreamEvent("thinking", content, null, null, null, null);
    }

    /**
     * 创建文本事件
     */
    public static ChatStreamEvent text(String content) {
        return new ChatStreamEvent("text", content, null, null, null, null);
    }

    /**
     * 创建完成事件
     */
    public static ChatStreamEvent completion(String fullText, String fullThinking, long durationMs, String conversationId) {
        return new ChatStreamEvent("completion", null, fullText, fullThinking, durationMs, conversationId);
    }

    /**
     * 创建错误事件
     */
    public static ChatStreamEvent error(String content) {
        return new ChatStreamEvent("error", content, null, null, null, null);
    }
}
