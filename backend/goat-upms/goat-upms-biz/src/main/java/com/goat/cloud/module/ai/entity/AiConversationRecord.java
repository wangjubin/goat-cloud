package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话记录实体
 */
@Data
@TableName("ai_conversation_record")
public class AiConversationRecord {

    @TableId(type = IdType.AUTO)
    private Long recordId;

    private String conversationId;

    private Long agentId;

    private Long userId;

    private String role;

    private String content;

    private String thinking;

    private Integer tokenCount;

    private String status;

    private LocalDateTime createTime;

    private Integer deleted;
}
