package com.goat.cloud.module.ai.agent;

/**
 * 智能体对话责任链 Handler 接口
 * 每个 Handler 完成自己的职责后，若 ctx.terminated == true 则短路
 */
public interface AgentChatHandler {

    /**
     * 处理上下文，填充自身职责范围内的数据
     */
    void handle(AgentChatContext ctx);

    /**
     * Handler 顺序，默认 100
     */
    default int order() {
        return 100;
    }
}
