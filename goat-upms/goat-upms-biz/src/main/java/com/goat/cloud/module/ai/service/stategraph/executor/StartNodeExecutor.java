package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 起始节点执行器
 * <p>
 * DAG 工作流的入口点，将输入参数初始化到上下文
 * @author wangjubin
 */
@Component
@RequiredArgsConstructor
public class StartNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "START";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "STARTED");
            result.put("inputKeys", context.keySet());
            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            return NodeResult.ok("{\"status\":\"STARTED\"}");
        }
    }
}