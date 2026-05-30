package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.framework.config.JsonbTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName(value = "ai_state_trace", autoResultMap = true)
public class AiStateTrace implements Serializable {

    @TableId(value = "trace_id", type = IdType.AUTO)
    private Long traceId;
    private Long sessionId;
    private Long nodeId;
    private String nodeCode;
    private String nodeType;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String inputJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String outputJson;
    private String status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private Long createBy;
    private LocalDateTime createTime;
    private Integer deleted;
}
