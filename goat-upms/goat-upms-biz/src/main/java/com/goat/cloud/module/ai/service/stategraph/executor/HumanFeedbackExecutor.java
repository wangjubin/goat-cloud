package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 人工反馈节点执行器 (HITL - Human-in-the-loop)
 * <p>
 * 中断执行流程，等待用户确认或修正 SQL/结果后继续
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HumanFeedbackExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "HUMAN_FEEDBACK";
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            String generatedSql = (String) context.get("generatedSql");
            Object sqlResult = context.get("sqlResult");

            Map<String, Object> interruptData = new LinkedHashMap<>();
            interruptData.put("message", "请确认以下SQL查询和结果是否正确");
            interruptData.put("generatedSql", generatedSql);
            interruptData.put("sqlResult", sqlResult);
            interruptData.put("options", List.of(
                    Map.of("value", "APPROVE", "label", "确认，继续执行"),
                    Map.of("value", "MODIFY_SQL", "label", "修改SQL"),
                    Map.of("value", "REJECT", "label", "拒绝，重新生成"),
                    Map.of("value", "APPROVE_WITH_CHART", "label", "确认并生成图表")
            ));

            log.info("HITL interrupt: waiting for human feedback on SQL: {}", generatedSql);

            return NodeResult.interrupt(objectMapper.writeValueAsString(interruptData));
        } catch (Exception e) {
            log.error("Human feedback node error", e);
            return NodeResult.fail("Human feedback setup failed: " + e.getMessage());
        }
    }
}