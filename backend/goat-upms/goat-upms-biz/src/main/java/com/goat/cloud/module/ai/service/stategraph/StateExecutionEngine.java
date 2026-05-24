package com.goat.cloud.module.ai.service.stategraph;

import com.goat.cloud.module.ai.entity.AiStateSession;
import com.goat.cloud.module.ai.entity.AiStateTrace;

import java.util.List;
import java.util.Map;

/**
 * StateGraph 执行引擎接口
 * <p>
 * 核心引擎负责：加载图定义 → 按顺序执行节点 → 支持中断/恢复 → 记录追踪
 * @author wangjubin
 */
public interface StateExecutionEngine {

    /**
     * 启动一个新的 StateGraph 执行会话
     *
     * @param graphCode 图定义编码
     * @param userId    用户ID
     * @param input     初始输入（用户问题等）
     * @return 执行会话
     */
    AiStateSession startSession(String graphCode, Long userId, Map<String, Object> input);

    /**
     * 恢复被中断的会话（如人工反馈后继续执行）
     *
     * @param runId  运行ID
     * @param feedback 用户反馈数据
     * @return 更新后的会话
     */
    AiStateSession resumeSession(String runId, Map<String, Object> feedback);

    /**
     * 获取会话状态
     */
    AiStateSession getSession(String runId);

    /**
     * 通过 sessionId 获取会话
     */
    AiStateSession getSessionById(Long sessionId);

    /**
     * 取消会话
     */
    void cancelSession(String runId);

    /**
     * 获取会话的节点执行追踪记录
     */
    List<AiStateTrace> getSessionTraces(Long sessionId);

    /**
     * 注册节点执行器
     */
    void registerExecutor(NodeExecutor executor);

    /**
     * 流式执行（用于 SSE），通过回调逐步输出节点执行结果
     *
     * @param graphCode 图定义编码
     * @param userId    用户ID
     * @param input     初始输入
     * @param callback  节点执行结果回调
     * @return 最终会话状态
     */
    AiStateSession executeStreaming(String graphCode, Long userId, Map<String, Object> input,
                                    StreamCallback callback);

    /**
     * 流式回调接口
     */
    interface StreamCallback {
        /**
         * 节点开始执行
         */
        void onNodeStart(String nodeCode, String nodeType);

        /**
         * 节点执行完成
         */
        void onNodeComplete(String nodeCode, String nodeType, String outputJson);

        /**
         * 节点执行失败
         */
        void onNodeError(String nodeCode, String nodeType, String errorMessage);

        /**
         * 会话中断（等待人工反馈）
         */
        void onInterrupt(String nodeCode, String reason);

        /**
         * 全部完成
         */
        void onComplete(AiStateSession session);
    }
}
