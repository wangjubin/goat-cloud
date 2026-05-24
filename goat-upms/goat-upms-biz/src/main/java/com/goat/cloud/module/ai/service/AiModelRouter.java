package com.goat.cloud.module.ai.service;

import com.goat.cloud.module.ai.entity.AiModelConfig;

import java.util.List;

/**
 * 模型路由服务接口
 * @author wangjubin
 */
public interface AiModelRouter {

    /**
     * 按类型路由
     */
    AiModelConfig routeByType(String modelType);

    /**
     * 按能力路由
     */
    AiModelConfig routeByCapability(String capability);

    /**
     * 负载均衡路由
     */
    AiModelConfig routeByLoad(String modelType);

    /**
     * 获取指定类型的所有可用模型
     */
    List<AiModelConfig> getModelsByType(String modelType);

    /**
     * 获取指定provider的所有可用模型
     */
    List<AiModelConfig> getModelsByProvider(String provider);
}
