package com.goat.cloud.module.ai.service;

import com.goat.cloud.module.ai.entity.AiMcpTool;

import java.util.Map;
import java.util.List;

/**
 * MCP客户端服务接口
 * @author wangjubin
 */
public interface AiMcpClient {

    /**
     * MCP传输类型
     */
    enum TransportType {
        STDIO,
        HTTP,
        SSE
    }

    /**
     * 调用MCP工具
     */
    ToolCallResult callTool(AiMcpTool tool, Map<String, Object> parameters);

    /**
     * 批量调用MCP工具
     */
    List<ToolCallResult> batchCallTools(List<AiMcpTool> tools, List<Map<String, Object>> parametersList);

    /**
     * 健康检查
     */
    HealthCheckResult healthCheck(AiMcpTool tool);

    /**
     * 获取工具描述
     */
    ToolDescription describeTool(AiMcpTool tool);

    /**
     * 工具调用结果
     */
    record ToolCallResult(
            Long toolId,
            String toolCode,
            boolean success,
            String result,
            String error,
            long executionTimeMs
    ) {}

    /**
     * 健康检查结果
     */
    record HealthCheckResult(
            Long toolId,
            boolean healthy,
            String message,
            long latencyMs
    ) {}

    /**
     * 工具描述
     */
    record ToolDescription(
            String name,
            String description,
            List<ParameterInfo> parameters
    ) {}

    /**
     * 参数信息
     */
    record ParameterInfo(
            String name,
            String type,
            String description,
            boolean required
    ) {}
}
