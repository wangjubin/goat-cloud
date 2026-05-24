package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 终止节点执行器
 * <p>
 * DAG 工作流的出口点，汇总最终结果
 * @author wangjubin
 */
@Component
@RequiredArgsConstructor
public class EndNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "END";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "COMPLETED");
            result.put("contextKeys", context.keySet());
            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            return NodeResult.ok("{\"status\":\"COMPLETED\"}");
        }
    }
}