package com.goat.cloud.module.ai.agent.handler;

import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.entity.AiPromptTemplate;
import com.goat.cloud.module.ai.mapper.AiPromptTemplateMapper;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 系统提示词初始化：以 Agent instruction 作为 systemPrompt 基础
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemPromptHandler implements AgentChatHandler {

    private final AiPromptTemplateMapper promptTemplateMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) return;

        String instruction = ctx.getAgent().getDescription();
        if (!StringUtils.hasText(instruction)) {
            instruction = "You are a helpful AI assistant.";
        }

        // 加载关联的提示词模板
        Long promptId = ctx.getAgent().getPromptId();
        if (promptId != null) {
            try {
                AiPromptTemplate template = promptTemplateMapper.selectById(promptId);
                if (template != null && StringUtils.hasText(template.getSystemPrompt())) {
                    instruction = template.getSystemPrompt();
                }
            } catch (Exception ex) {
                // 模板加载失败，使用默认 instruction
            }
        }

        ctx.setSystemPrompt(instruction);
        log.debug("SystemPromptHandler: prompt length={}", instruction.length());
    }

    @Override
    public int order() {
        return 50;
    }
}
