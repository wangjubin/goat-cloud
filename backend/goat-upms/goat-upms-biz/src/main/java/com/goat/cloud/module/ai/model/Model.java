package com.goat.cloud.module.ai.model;

import com.goat.cloud.module.ai.entity.AiModelConfig;

/**
 * 模型接口（参考 snail-ai Model 接口）
 * 所有模型类型（Chat、Embedding、Rerank）的公共接口
 */
public interface Model {

    /**
     * 判断是否支持指定的模型 key
     */
    boolean supports(String modelKey);

    /**
     * 设置模型配置
     */
    void setModelConfig(AiModelConfig config);

    /**
     * 获取模型配置
     */
    AiModelConfig getModelConfig();
}
