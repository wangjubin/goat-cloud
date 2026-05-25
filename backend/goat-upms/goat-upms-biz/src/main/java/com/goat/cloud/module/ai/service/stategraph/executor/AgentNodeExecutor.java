package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.runtime.AiAgentService;
import com.goat.cloud.module.ai.runtime.model.AgentRunRequest;
import com.goat.cloud.module.ai.runtime.model.AgentRunResponse;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent 节点执行器
 * <p>
 * 在工作流中调用 Agent 运行，支持知识库检索、工具调用和对话生成
 * <p>
 * 节点配置示例：
 * {
 *   "agentId": 1
 * }
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentNodeExecutor implements NodeExecutor {

    private final AiAgentService agentService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "AGENT";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            if (nodeConfig != null && !nodeConfig.isBlank()) {
                config = objectMapper.readValue(nodeConfig, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            }

            // Resolve agentId: config first, then context fallback
            Long agentId = toLong(config.get("agentId"));
            if (agentId == null) {
                agentId = toLong(context.get("agentId"));
            }

            // If no agentId found, skip with status
            if (agentId == null) {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("status", "SKIPPED");
                output.put("reason", "No agentId provided in config or context");
                return NodeResult.ok(objectMapper.writeValueAsString(output));
            }

            // Build agent run request
            AgentRunRequest agentRequest = new AgentRunRequest();
            agentRequest.setMessage(asString(context.get("userMessage")));
            agentRequest.setConversationId(asString(context.get("conversationId")));

            AgentRunResponse agentResponse = agentService.runAgent(agentId, agentRequest);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("agentId", agentResponse.getAgentId());
            output.put("agentCode", agentResponse.getAgentCode());
            output.put("chat", agentResponse.getChat());

            context.put("agentResult", output);

            return NodeResult.ok(objectMapper.writeValueAsString(output));

        } catch (Exception e) {
            log.error("Agent node execution error", e);
            return NodeResult.fail("Agent execution failed: " + e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }
}