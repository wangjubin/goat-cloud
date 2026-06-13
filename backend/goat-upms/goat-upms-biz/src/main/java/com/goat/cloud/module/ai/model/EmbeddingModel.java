package com.goat.cloud.module.ai.model;

import java.util.List;

/**
 * Embedding 模型接口
 * 支持文本向量化
 */
public interface EmbeddingModel extends Model {

    /**
     * 单个文本向量化
     */
    float[] embed(String text);

    /**
     * 批量文本向量化
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     */
    int getDimensions();
}
