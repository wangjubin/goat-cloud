package com.goat.cloud.module.ai.memory;

import java.util.List;

/**
 * 短期记忆存储接口
 * 支持滑动窗口的对话历史存储
 */
public interface ShortTermMemoryStore {

    /**
     * 追加一条消息到短期记忆
     */
    void append(String conversationId, String role, String content, int windowSize);

    /**
     * 加载历史消息（不含最新一条）
     */
    List<ShortTermMessage> loadHistory(String conversationId, int windowSize);

    /**
     * 清除指定会话的短期记忆
     */
    void evict(String conversationId);

    /**
     * 存储类型标识
     */
    String storeType();
}
