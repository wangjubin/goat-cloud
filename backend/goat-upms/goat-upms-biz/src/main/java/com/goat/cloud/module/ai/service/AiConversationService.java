package com.goat.cloud.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.module.ai.entity.AiConversation;
import com.goat.cloud.module.ai.entity.AiConversationRecord;
import com.goat.cloud.module.ai.mapper.AiConversationMapper;
import com.goat.cloud.module.ai.mapper.AiConversationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI 会话持久化服务
 * 管理会话创建、消息存储、历史加载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final AiConversationMapper conversationMapper;
    private final AiConversationRecordMapper recordMapper;

    /**
     * 创建或获取会话
     */
    public AiConversation getOrCreateConversation(String conversationId, Long agentId, Long userId, String title) {
        if (StringUtils.hasText(conversationId)) {
            AiConversation existing = conversationMapper.selectById(conversationId);
            if (existing != null) {
                return existing;
            }
        }

        String id = StringUtils.hasText(conversationId) ? conversationId : UUID.randomUUID().toString();
        AiConversation conversation = new AiConversation();
        conversation.setConversationId(id);
        conversation.setAgentId(agentId);
        conversation.setUserId(userId);
        conversation.setTitle(truncate(title, 128));
        conversation.setStatus("ACTIVE");
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversation.setDeleted(0);
        conversationMapper.insert(conversation);
        log.debug("Created conversation: {}", id);
        return conversation;
    }

    /**
     * 保存消息
     */
    public void saveMessage(String conversationId, Long agentId, Long userId,
                            String role, String content, String thinking) {
        AiConversationRecord record = new AiConversationRecord();
        record.setConversationId(conversationId);
        record.setAgentId(agentId);
        record.setUserId(userId);
        record.setRole(role);
        record.setContent(content);
        record.setThinking(thinking);
        record.setTokenCount(estimateTokens(content));
        record.setStatus("SUCCESS");
        record.setCreateTime(LocalDateTime.now());
        record.setDeleted(0);
        recordMapper.insert(record);
    }

    /**
     * 加载历史消息（时间正序，排除最新一条）
     */
    public List<AiConversationRecord> getHistory(String conversationId, int limit) {
        if (!StringUtils.hasText(conversationId)) {
            return List.of();
        }
        LambdaQueryWrapper<AiConversationRecord> wrapper = new LambdaQueryWrapper<AiConversationRecord>()
                .eq(AiConversationRecord::getConversationId, conversationId)
                .eq(AiConversationRecord::getDeleted, 0)
                .orderByAsc(AiConversationRecord::getCreateTime)
                .last("LIMIT " + (limit + 1));
        List<AiConversationRecord> all = recordMapper.selectList(wrapper);
        // 排除最后一条（当前用户消息，已在 prompt 中）
        if (all.size() <= 1) {
            return List.of();
        }
        return all.subList(0, all.size() - 1);
    }

    /**
     * 删除会话（软删除）
     */
    public void deleteConversation(String conversationId) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setDeleted(1);
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conversation);
            log.info("Deleted conversation: {}", conversationId);
        }
    }

    /**
     * 更新会话标题
     */
    public void updateConversationTitle(String conversationId, String title) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setTitle(truncate(title, 128));
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conversation);
            log.debug("Updated conversation title: {}", conversationId);
        }
    }

    /**
     * 获取会话列表
     */
    public List<AiConversation> listConversations(Long agentId, Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getAgentId, agentId)
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getDeleted, 0)
                .orderByDesc(AiConversation::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + (pageNum - 1) * pageSize);
        return conversationMapper.selectList(wrapper);
    }

    /**
     * 获取会话详情
     */
    public AiConversation getConversation(String conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return Math.max(1, text.length() / 4);
    }
}
