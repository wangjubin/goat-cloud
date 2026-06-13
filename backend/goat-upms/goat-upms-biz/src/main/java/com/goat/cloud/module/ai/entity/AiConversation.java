package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话实体
 */
@Data
@TableName("ai_conversation")
public class AiConversation {

    @TableId(type = IdType.INPUT)
    private String conversationId;

    private Long agentId;

    private Long userId;

    private String title;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
