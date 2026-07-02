package com.goat.cloud.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.service.AiModelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 模型路由器实现
 * 支持负载均衡、自动降级、Fallback机制
 * 
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelRouterImpl implements AiModelRouter {

    private final AiModelConfigMapper modelConfigMapper;
    
    // 轮询计数器
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    
    // 模型健康状态缓存 (modelId -> isHealthy)
    private final Map<Long, Boolean> modelHealthStatus = new ConcurrentHashMap<>();
    
    // 模型性能指标 (modelId -> avgLatencyMs)
    private final Map<Long, Long> modelPerformanceMetrics = new ConcurrentHashMap<>();

    @Override
    public AiModelConfig routeByType(String modelType) {
        if (!StringUtils.hasText(modelType)) {
            log.warn("Model type is empty, using default model");
            return getDefaultModel();
        }
        
        List<AiModelConfig> availableModels = getAvailableModelsByType(modelType);
        if (availableModels.isEmpty()) {
            log.error("No available models for type: {}", modelType);
            return getDefaultModel();
        }
        
        // 优先返回默认模型
        return availableModels.stream()
                .filter(m -> Boolean.TRUE.equals(m.getDefaultModel()))
                .findFirst()
                .orElse(availableModels.get(0));
    }

    @Override
    public AiModelConfig routeByCapability(String capability) {
        if (!StringUtils.hasText(capability)) {
            return getDefaultModel();
        }
        
        List<AiModelConfig> allModels = getAllAvailableModels();
        
        // 查找具有指定能力标签的模型
        List<AiModelConfig> matchedModels = allModels.stream()
                .filter(m -> hasCapability(m, capability))
                .collect(Collectors.toList());
        
        if (matchedModels.isEmpty()) {
            log.warn("No models found with capability: {}", capability);
            return getDefaultModel();
        }
        
        // 使用负载均衡选择最佳模型
        return selectBestModelByLoad(matchedModels);
    }

    @Override
    public AiModelConfig routeByLoad(String modelType) {
        List<AiModelConfig> availableModels = getAvailableModelsByType(modelType);
        if (availableModels.isEmpty()) {
            return getDefaultModel();
        }
        
        return selectBestModelByLoad(availableModels);
    }

    @Override
    public List<AiModelConfig> getModelsByType(String modelType) {
        return getAvailableModelsByType(modelType);
    }

    @Override
    public List<AiModelConfig> getModelsByProvider(String provider) {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getProvider, provider)
               .eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
               .orderByAsc(AiModelConfig::getSortOrder);
        return modelConfigMapper.selectList(wrapper);
    }

    /**
     * 智能路由：综合考虑健康状态、性能指标、负载均衡
     */
    public AiModelConfig smartRoute(String modelType, String preferredProvider) {
        List<AiModelConfig> candidates = getAvailableModelsByType(modelType);
        
        if (candidates.isEmpty()) {
            log.error("No available models for type: {}", modelType);
            return getDefaultModel();
        }
        
        // 1. 过滤健康模型
        List<AiModelConfig> healthyModels = candidates.stream()
                .filter(m -> isModelHealthy(m.getModelId()))
                .collect(Collectors.toList());
        
        if (healthyModels.isEmpty()) {
            log.warn("No healthy models available, falling back to all candidates");
            healthyModels = candidates;
        }
        
        // 2. 如果指定了provider，优先选择
        if (StringUtils.hasText(preferredProvider)) {
            List<AiModelConfig> providerModels = healthyModels.stream()
                    .filter(m -> preferredProvider.equals(m.getProvider()))
                    .collect(Collectors.toList());
            if (!providerModels.isEmpty()) {
                healthyModels = providerModels;
            }
        }
        
        // 3. 负载均衡选择
        return selectBestModelByLoad(healthyModels);
    }

    /**
     * 带Fallback的路由：主模型失败时自动切换
     */
    public AiModelConfig routeWithFallback(String modelType, List<String> fallbackProviders) {
        // 首先尝试主模型类型
        AiModelConfig primary = routeByLoad(modelType);
        if (primary != null && isModelHealthy(primary.getModelId())) {
            return primary;
        }
        
        // 主模型不可用，尝试fallback链
        if (fallbackProviders != null && !fallbackProviders.isEmpty()) {
            for (String provider : fallbackProviders) {
                List<AiModelConfig> providerModels = getModelsByProvider(provider);
                if (!providerModels.isEmpty()) {
                    AiModelConfig fallback = selectBestModelByLoad(providerModels);
                    if (fallback != null && isModelHealthy(fallback.getModelId())) {
                        log.info("Using fallback model from provider: {}", provider);
                        return fallback;
                    }
                }
            }
        }
        
        // 最终fallback到默认模型
        log.warn("All fallbacks failed, using default model");
        return getDefaultModel();
    }

    /**
     * 更新模型健康状态
     */
    public void updateModelHealth(Long modelId, boolean healthy) {
        modelHealthStatus.put(modelId, healthy);
        log.debug("Updated model {} health status: {}", modelId, healthy);
    }

    /**
     * 更新模型性能指标
     */
    public void updateModelPerformance(Long modelId, long latencyMs) {
        modelPerformanceMetrics.put(modelId, latencyMs);
    }

    /**
     * 检查模型是否健康
     */
    private boolean isModelHealthy(Long modelId) {
        // 如果没有记录，默认认为健康
        return modelHealthStatus.getOrDefault(modelId, true);
    }

    /**
     * 根据负载均衡选择最佳模型
     */
    private AiModelConfig selectBestModelByLoad(List<AiModelConfig> models) {
        if (models.isEmpty()) {
            return null;
        }
        
        if (models.size() == 1) {
            return models.get(0);
        }
        
        // 策略1: 优先选择性能最好的模型
        Optional<AiModelConfig> bestPerformance = models.stream()
                .filter(m -> modelPerformanceMetrics.containsKey(m.getModelId()))
                .min(Comparator.comparingLong(m -> modelPerformanceMetrics.get(m.getModelId())));
        
        if (bestPerformance.isPresent()) {
            return bestPerformance.get();
        }
        
        // 策略2: 轮询负载均衡
        String key = models.get(0).getModelType();
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % models.size();
        
        return models.get(index);
    }

    /**
     * 获取指定类型的所有可用模型
     */
    private List<AiModelConfig> getAvailableModelsByType(String modelType) {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getModelType, modelType)
               .eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
               .orderByAsc(AiModelConfig::getSortOrder);
        return modelConfigMapper.selectList(wrapper);
    }

    /**
     * 获取所有可用模型
     */
    private List<AiModelConfig> getAllAvailableModels() {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
               .orderByAsc(AiModelConfig::getSortOrder);
        return modelConfigMapper.selectList(wrapper);
    }

    /**
     * 获取默认模型
     */
    private AiModelConfig getDefaultModel() {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getDefaultModel, true)
               .eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
               .last("LIMIT 1");
        AiModelConfig defaultModel = modelConfigMapper.selectOne(wrapper);
        
        if (defaultModel == null) {
            // 如果没有默认模型，返回第一个可用模型
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiModelConfig::getStatus, CommonStatus.ENABLED)
                   .orderByAsc(AiModelConfig::getSortOrder)
                   .last("LIMIT 1");
            defaultModel = modelConfigMapper.selectOne(wrapper);
        }
        
        return defaultModel;
    }

    /**
     * 检查模型是否具有指定能力
     */
    private boolean hasCapability(AiModelConfig model, String capability) {
        String tags = model.getCapabilityTags();
        if (!StringUtils.hasText(tags)) {
            return false;
        }
        // 能力标签以逗号分隔
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .anyMatch(tag -> tag.equalsIgnoreCase(capability));
    }
}
