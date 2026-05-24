package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.service.AiMcpServerService;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP 工具调用节点执行器
 * <p>
 * 在工作流中调用外部 MCP 服务器上的工具
 * <p>
 * 节点配置示例：
 * {
 *   "serverId": 1,
 *   "toolName": "query_database",
 *   "arguments": {"sql": "{{generatedSql}}"},
 *   "timeoutMs": 30000
 * }
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolNodeExecutor implements NodeExecutor {

    private final AiMcpServerService mcpServerService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "MCP_TOOL";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        if (nodeConfig == null || nodeConfig.isBlank()) {
            return NodeResult.fail("MCP tool node config is empty");
        }

        try {
            Map<String, Object> config = objectMapper.readValue(nodeConfig, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            Long serverId = toLong(config.get("serverId"));
            String toolName = (String) config.get("toolName");
            @SuppressWarnings("unchecked")
            Map<String, Object> argumentTemplate = (Map<String, Object>) config.get("arguments");

            if (serverId == null) {
                return NodeResult.fail("serverId is required in MCP tool config");
            }
            if (toolName == null || toolName.isBlank()) {
                return NodeResult.fail("toolName is required in MCP tool config");
            }

            // Resolve template variables in arguments (e.g. {{generatedSql}})
            Map<String, Object> resolvedArgs = resolveArguments(argumentTemplate, context);

            // Call MCP tool
            String result = mcpServerService.callTool(serverId, toolName, resolvedArgs);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("serverId", serverId);
            output.put("toolName", toolName);
            output.put("result", result);

            context.put("mcpToolResult", output);
            return NodeResult.ok(objectMapper.writeValueAsString(output));

        } catch (Exception e) {
            log.error("MCP tool node execution error", e);
            return NodeResult.fail("MCP tool call failed: " + e.getMessage());
        }
    }

    /**
     * Resolve template variables like {{variableName}} in argument values
     */
    private Map<String, Object> resolveArguments(Map<String, Object> template, Map<String, Object> context) {
        if (template == null) return Map.of();

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str) {
                resolved.put(entry.getKey(), resolveTemplate(str, context));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    private String resolveTemplate(String template, Map<String, Object> context) {
        if (template == null) return null;

        // Handle {{var}} patterns
        String result = template;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{(\\w+)}}").matcher(template);
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            if (value != null) {
                result = result.replace("{{" + varName + "}}", String.valueOf(value));
            }
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}