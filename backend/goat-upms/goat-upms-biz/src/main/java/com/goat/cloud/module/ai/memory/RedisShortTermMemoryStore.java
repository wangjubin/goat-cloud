package com.goat.cloud.module.ai.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 短期记忆存储实现
 * 使用 Redis List 存储，LPUSH + LTRIM 维护滑动窗口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisShortTermMemoryStore implements ShortTermMemoryStore {

    private static final String KEY_PREFIX = "goat:ai:memory:";
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void append(String conversationId, String role, String content, int windowSize) {
        String key = KEY_PREFIX + conversationId;
        String message = role + "|||" + content;
        redisTemplate.opsForList().leftPush(key, message);
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        // 维护滑动窗口
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > windowSize) {
            redisTemplate.opsForList().trim(key, 0, windowSize - 1);
        }
    }

    @Override
    public List<ShortTermMessage> loadHistory(String conversationId, int windowSize) {
        String key = KEY_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return List.of();
        }
        // 获取所有消息（时间正序：从右到左取，反转）
        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        // 反转为时间正序
        List<ShortTermMessage> result = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            String msg = messages.get(i);
            String[] parts = msg.split("\\|\\|\\|", 2);
            if (parts.length == 2) {
                result.add(new ShortTermMessage(parts[0], parts[1]));
            }
        }
        // 排除最后一条（当前用户消息）
        if (result.size() <= 1) {
            return List.of();
        }
        return result.subList(0, result.size() - 1);
    }

    @Override
    public void evict(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        redisTemplate.delete(key);
    }

    @Override
    public String storeType() {
        return "redis";
    }
}
