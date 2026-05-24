package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_sql_log")
public class AiSqlLog implements Serializable {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;
    private Long sessionId;
    private String question;
    private String generatedSql;
    private String intentResult;
    private String schemaContext;
    private String llmConfigJson;
    private String executionResult;
    private Long executionTimeMs;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
    private Integer deleted;
}
