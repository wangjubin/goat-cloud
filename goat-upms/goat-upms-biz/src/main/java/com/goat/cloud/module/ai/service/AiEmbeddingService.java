package com.goat.cloud.module.ai.service;

import java.util.List;

/**
 * 向量化服务接口
 * @author wangjubin
 */
public interface AiEmbeddingService {

    /**
     * 生成单个文本的向量
     */
    float[] generateEmbedding(String text);

    /**
     * 批量生成向量
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * 获取向量维度
     */
    int getEmbeddingDimension();

    /**
     * 计算余弦相似度
     */
    double cosineSimilarity(float[] vec1, float[] vec2);
}
