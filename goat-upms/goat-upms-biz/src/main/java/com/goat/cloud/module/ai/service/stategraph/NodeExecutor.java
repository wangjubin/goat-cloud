package com.goat.cloud.module.ai.service.stategraph;

import java.util.Map;

/**
 * StateGraph 节点执行器接口
 * <p>
 * 每种节点类型（意图识别、Schema召回、NL2SQL、SQL执行、人工反馈、Python执行、报告生成）
 * 实现此接口
 * @author wangjubin
 */
public interface NodeExecutor {

    /**
     * 节点类型标识，如 INTENT_RECOGNITION, SCHEMA_RECALL, NL2SQL, SQL_EXECUTION,
     * HUMAN_FEEDBACK, PYTHON_EXECUTION, REPORT_GENERATION
     */
    String getNodeType();

    /**
     * 执行节点逻辑
     *
     * @param context 会话上下文，包含之前节点的输出
     * @param nodeConfig 节点配置 JSON
     * @return 节点输出结果
     */
    NodeResult execute(Map<String, Object> context, String nodeConfig);

    /**
     * 是否为中断节点（如人工反馈节点需要中断等待用户输入）
     */
    default boolean isInterruptible() {
        return false;
    }
}
