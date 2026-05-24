package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_state_session")
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
    private String interruptData;
    private String contextJson;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime interruptedAt;
    private LocalDateTime completedAt;
}
