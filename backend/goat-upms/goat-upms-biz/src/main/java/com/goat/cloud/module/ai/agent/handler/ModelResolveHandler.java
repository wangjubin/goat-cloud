package com.goat.cloud.module.ai.agent.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 模型解析：确定对话使用的模型，解析 API Key
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelResolveHandler implements AgentChatHandler {

    private final AiModelConfigMapper modelConfigMapper;
    private final Environment environment;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) return;

        Long modelId = ctx.getAgent().getModelId();
        AiModelConfig model = null;

        // 按 ID 查找
        if (modelId != null) {
            try { model = modelConfigMapper.selectById(modelId); }
            catch (Exception e) { /* ignore missing table */ }
        }

        // 按默认 CHAT 类型查找
        if (model == null) {
            try {
                model = modelConfigMapper.selectOne(new QueryWrapper<AiModelConfig>()
                        .eq("model_type", "CHAT")
                        .eq("default_model", true)
                        .eq("status", "ENABLED")
                        .orderByAsc("sort_order")
                        .last("limit 1"));
            } catch (Exception e) { /* ignore */ }
        }

        // 兜底：任意启用模型
        if (model == null) {
            try {
                model = modelConfigMapper.selectOne(new QueryWrapper<AiModelConfig>()
                        .eq("status", "ENABLED")
                        .orderByAsc("sort_order")
                        .last("limit 1"));
            } catch (Exception e) { /* ignore */ }
        }

        if (model == null) {
            ctx.terminate("No enabled chat model configuration found");
            return;
        }

        ctx.setModelConfig(model);
        ctx.getRuntimeMetadata().put("modelId", model.getModelId());
        ctx.getRuntimeMetadata().put("modelName", model.getModelName());
        ctx.getRuntimeMetadata().put("provider", model.getProvider());
        log.debug("ModelResolve: resolved model {} ({})", model.getModelName(), model.getModelCode());
    }

    @Override
    public int order() {
        return 30;
    }
}
