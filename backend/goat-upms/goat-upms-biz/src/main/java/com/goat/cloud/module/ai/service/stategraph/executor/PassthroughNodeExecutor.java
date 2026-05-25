package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Passthrough 节点执行器
 * <p>
 * 在工作流中透传上下文数据，不做任何业务处理，仅将 context 中的键列表输出
 * <p>
 * 节点配置示例：
 * {}
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PassthroughNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "PASSTHROUGH";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            List<String> contextKeys = new ArrayList<>(context.keySet());

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("passthrough", true);
            output.put("contextKeys", contextKeys);

            return NodeResult.ok(objectMapper.writeValueAsString(output));

        } catch (Exception e) {
            log.error("Passthrough node execution error", e);
            return NodeResult.fail("Passthrough failed: " + e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }
}