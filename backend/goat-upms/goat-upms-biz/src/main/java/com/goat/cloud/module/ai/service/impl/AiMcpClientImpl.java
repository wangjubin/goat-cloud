package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiMcpTool;
import com.goat.cloud.module.ai.service.AiMcpClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * MCP客户端服务实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiMcpClientImpl implements AiMcpClient {

    @Resource
    private RestTemplate restTemplate;

    @Override
    public ToolCallResult callTool(AiMcpTool tool, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();

        try {
            TransportType transportType = parseTransportType(tool.getTransportType());

            return switch (transportType) {
                case HTTP -> callHttpTool(tool, parameters, startTime);
                case STDIO -> callStdioTool(tool, parameters, startTime);
                case SSE -> callSseTool(tool, parameters, startTime);
            };
        } catch (Exception e) {
            log.error("Failed to call MCP tool: {}", tool.getToolCode(), e);
            return new ToolCallResult(
                    tool.getMcpToolId(),
                    tool.getToolCode(),
                    false,
                    null,
                    e.getMessage(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    @Override
    public List<ToolCallResult> batchCallTools(List<AiMcpTool> tools, List<Map<String, Object>> parametersList) {
        List<ToolCallResult> results = new ArrayList<>();

        for (int i = 0; i < tools.size(); i++) {
            AiMcpTool tool = tools.get(i);
            Map<String, Object> params = parametersList != null && i < parametersList.size()
                    ? parametersList.get(i) : Map.of();
            results.add(callTool(tool, params));
        }

        return results;
    }

    @Override
    public HealthCheckResult healthCheck(AiMcpTool tool) {
        long startTime = System.currentTimeMillis();

        try {
            TransportType transportType = parseTransportType(tool.getTransportType());

            return switch (transportType) {
                case HTTP -> healthCheckHttp(tool, startTime);
                case STDIO -> healthCheckStdio(tool, startTime);
                case SSE -> healthCheckSse(tool, startTime);
            };
        } catch (Exception e) {
            log.error("Health check failed for MCP tool: {}", tool.getToolCode(), e);
            return new HealthCheckResult(
                    tool.getMcpToolId(),
                    false,
                    e.getMessage(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    @Override
    public ToolDescription describeTool(AiMcpTool tool) {
        try {
            Map<String, Object> inputSchema = parseJsonSchema(tool.getInputSchema());

            @SuppressWarnings("unchecked")
            List<ParameterInfo> params = parseParameters((Map<String, Object>) inputSchema.get("properties"));

            return new ToolDescription(
                    tool.getToolCode(),
                    tool.getToolName(),
                    params
            );
        } catch (Exception e) {
            log.warn("Failed to describe tool: {}", tool.getToolCode(), e);
            return new ToolDescription(tool.getToolCode(), tool.getToolName(), List.of());
        }
    }

    private ToolCallResult callHttpTool(AiMcpTool tool, Map<String, Object> parameters, long startTime) {
        String endpoint = tool.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return new ToolCallResult(
                    tool.getMcpToolId(),
                    tool.getToolCode(),
                    false,
                    null,
                    "Endpoint not configured",
                    System.currentTimeMillis() - startTime
            );
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("method", tool.getToolCode());
            requestBody.put("params", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = endpoint + (endpoint.endsWith("/") ? "" : "/") + "tools/" + tool.getToolCode();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            boolean success = response != null && !"error".equals(response.get("type"));
            String result = response != null ? response.get("result").toString() : null;
            String error = response != null && response.containsKey("error")
                    ? response.get("error").toString() : null;

            return new ToolCallResult(
                    tool.getMcpToolId(),
                    tool.getToolCode(),
                    success,
                    result,
                    error,
                    System.currentTimeMillis() - startTime
            );
        } catch (Exception e) {
            return new ToolCallResult(
                    tool.getMcpToolId(),
                    tool.getToolCode(),
                    false,
                    null,
                    e.getMessage(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    private ToolCallResult callStdioTool(AiMcpTool tool, Map<String, Object> parameters, long startTime) {
        log.warn("STDIO transport not supported for tool: {}", tool.getToolCode());
        return new ToolCallResult(
                tool.getMcpToolId(),
                tool.getToolCode(),
                false,
                null,
                "STDIO transport is not supported yet. Please use HTTP transport.",
                System.currentTimeMillis() - startTime
        );
    }

    private ToolCallResult callSseTool(AiMcpTool tool, Map<String, Object> parameters, long startTime) {
        log.warn("SSE transport not supported for tool: {}", tool.getToolCode());
        return new ToolCallResult(
                tool.getMcpToolId(),
                tool.getToolCode(),
                false,
                null,
                "SSE transport is not supported yet. Please use HTTP transport.",
                System.currentTimeMillis() - startTime
        );
    }

    private HealthCheckResult healthCheckHttp(AiMcpTool tool, long startTime) {
        String endpoint = tool.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return new HealthCheckResult(tool.getMcpToolId(), false, "No endpoint", System.currentTimeMillis() - startTime);
        }

        try {
            String url = endpoint + (endpoint.endsWith("/") ? "" : "/") + "health";
            restTemplate.getForObject(url, String.class);
            return new HealthCheckResult(tool.getMcpToolId(), true, "OK", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return new HealthCheckResult(tool.getMcpToolId(), false, e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    private HealthCheckResult healthCheckStdio(AiMcpTool tool, long startTime) {
        return new HealthCheckResult(tool.getMcpToolId(), true, "STDIO tool", System.currentTimeMillis() - startTime);
    }

    private HealthCheckResult healthCheckSse(AiMcpTool tool, long startTime) {
        return new HealthCheckResult(tool.getMcpToolId(), true, "SSE tool", System.currentTimeMillis() - startTime);
    }

    private TransportType parseTransportType(String type) {
        if (type == null) {
            return TransportType.HTTP;
        }
        try {
            return TransportType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TransportType.HTTP;
        }
    }

    private Map<String, Object> parseJsonSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            return Map.of("type", "object", "properties", Map.of());
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(schema, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse input schema: {}", schema);
            return Map.of("type", "object", "properties", Map.of());
        }
    }

    private List<ParameterInfo> parseParameters(Map<String, Object> properties) {
        List<ParameterInfo> params = new ArrayList<>();
        if (properties == null) {
            return params;
        }

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String name = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> prop = (Map<String, Object>) entry.getValue();
            String type = prop != null ? (String) prop.getOrDefault("type", "string") : "string";
            String description = prop != null ? (String) prop.getOrDefault("description", "") : "";
            boolean required = prop != null && Boolean.TRUE.equals(prop.get("required"));

            params.add(new ParameterInfo(name, type, description, required));
        }

        return params;
    }
}
