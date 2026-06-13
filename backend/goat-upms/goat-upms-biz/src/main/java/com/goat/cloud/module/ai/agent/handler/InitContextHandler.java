package com.goat.cloud.module.ai.agent.handler;

import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.entity.AiAgent;
import com.goat.cloud.module.ai.mapper.AiAgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 初始化：加载 Agent 实体，验证存在性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitContextHandler implements AgentChatHandler {

    private final AiAgentMapper agentMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        AiAgent agent = agentMapper.selectById(ctx.getAgentId());
        if (agent == null) {
            ctx.terminate("Agent not found: " + ctx.getAgentId());
            return;
        }
        ctx.setAgent(agent);
        log.debug("InitContext: loaded agent {}, code={}", agent.getAgentId(), agent.getAgentCode());
    }

    @Override
    public int order() {
        return 10;
    }
}
