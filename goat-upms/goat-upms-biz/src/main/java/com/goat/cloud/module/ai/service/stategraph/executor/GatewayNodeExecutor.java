package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 网关节点执行器
 * <p>
 * 支持三种网关类型：
 * - GATEWAY_AND: 所有分支必须完成（并行汇聚）
 * - GATEWAY_OR: 任一分支完成即可继续
 * - GATEWAY_XOR: 仅一条分支执行（排他选择）
 * <p>
 * 网关不执行实际逻辑，只做路由决策。条件在边的 condition 字段中定义。
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "GATEWAY";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Map<String, Object> config = nodeConfig != null && !nodeConfig.isBlank()
                    ? objectMapper.readValue(nodeConfig, new com.fasterxml.jackson.core.type.TypeReference<>() {})
                    : Map.of();

            String gatewayMode = (String) config.getOrDefault("mode", "XOR");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gatewayMode", gatewayMode);
            result.put("message", "Gateway routing decision: " + gatewayMode);

            // The actual routing is handled by the DAG executor's resolveNextNodes,
            // which evaluates edge conditions. The gateway just confirms the routing mode.
            context.put("gatewayMode", gatewayMode);

            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Gateway execution error", e);
            return NodeResult.fail("Gateway failed: " + e.getMessage());
        }
    }
}