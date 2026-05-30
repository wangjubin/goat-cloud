package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import com.goat.cloud.framework.config.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName(value = "ai_state_session", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
public class AiStateSession extends BaseEntity {

    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;
    private Long graphId;
    private String runId;
    private Long userId;
    private String conversationId;
    private String status;
    private Long currentNodeId;
    private String interruptReason;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String interruptData;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String contextJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String resultJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime interruptedAt;
    private LocalDateTime completedAt;
}
