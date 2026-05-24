package com.goat.cloud.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.service.AiModelRouter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模型路由服务实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiModelRouterImpl implements AiModelRouter {

    private final AtomicLong roundRobinCounter = new AtomicLong(0);

    @Resource
    private AiModelConfigMapper modelConfigMapper;

    @Override
    public AiModelConfig routeByType(String modelType) {
        List<AiModelConfig> models = getModelsByType(modelType);
        if (models.isEmpty()) {
            log.warn("No available models for type: {}", modelType);
            return null;
        }
        return models.stream()
                .filter(m -> m.getDefaultModel() != null && m.getDefaultModel())
                .findFirst()
                .orElse(models.get(0));
    }

    @Override
    public AiModelConfig routeByCapability(String capability) {
        List<AiModelConfig> enabled = modelConfigMapper.selectList(
                new LambdaQueryWrapper<AiModelConfig>()
                        .eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
                        .like(AiModelConfig::getCapabilityTags, capability)
                        .orderByAsc(AiModelConfig::getSortOrder)
        );
        if (enabled.isEmpty()) {
            log.warn("No available models with capability: {}", capability);
            return null;
        }
        return enabled.get(0);
    }

    @Override
    public AiModelConfig routeByLoad(String modelType) {
        List<AiModelConfig> models = getModelsByType(modelType);
        if (models.isEmpty()) {
            return null;
        }
        long index = roundRobinCounter.getAndIncrement() % models.size();
        return models.get((int) index);
    }

    @Override
    public List<AiModelConfig> getModelsByType(String modelType) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<AiModelConfig>()
                        .eq(AiModelConfig::getModelType, modelType)
                        .eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
                        .orderByAsc(AiModelConfig::getSortOrder)
        );
    }

    @Override
    public List<AiModelConfig> getModelsByProvider(String provider) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<AiModelConfig>()
                        .eq(AiModelConfig::getProvider, provider)
                        .eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
                        .orderByAsc(AiModelConfig::getSortOrder)
        );
    }
}
