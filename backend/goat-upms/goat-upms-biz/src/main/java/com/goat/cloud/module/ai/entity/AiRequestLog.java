package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI请求日志实体
 * 记录每次AI请求的详细信息，用于监控、审计和成本分析
 * 
 * @author wangjubin
 */
@Data
@TableName("ai_request_log")
public class AiRequestLog {

    @TableId(type = IdType.AUTO)
    private Long logId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 提供商
     */
    private String provider;

    /**
     * 业务类型 (chat/completion/embedding/etc)
     */
    private String bizType;

    /**
     * 请求ID (用于追踪)
     */
    private String requestId;

    /**
     * 输入Token数
     */
    private Integer promptTokens;

    /**
     * 输出Token数
     */
    private Integer completionTokens;

    /**
     * 总Token数
     */
    private Integer totalTokens;

    /**
     * 请求耗时(毫秒)
     */
    private Long latencyMs;

    /**
     * 请求状态 (SUCCESS/FAILED/TIMEOUT)
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 请求时间
     */
    private LocalDateTime requestTime;

    /**
     * 响应时间
     */
    private LocalDateTime responseTime;

    /**
     * 是否流式请求
     */
    private Boolean stream;

    /**
     * 额外元数据 (JSON格式)
     */
    private String metadata;
}
