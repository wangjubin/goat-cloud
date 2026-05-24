package com.goat.cloud.module.ai.service;

import com.goat.cloud.module.ai.entity.AiDocumentChunk;

import java.util.List;

/**
 * 检索服务接口
 * @author wangjubin
 */
public interface AiRetrievalService {

    /**
     * 检索结果
     */
    record RetrievalResult(
            AiDocumentChunk chunk,
            double score,
            int rank
    ) {}

    /**
     * 向量检索
     */
    List<RetrievalResult> vectorSearch(Long knowledgeBaseId, float[] queryEmbedding, int topK);

    /**
     * 混合检索(向量+关键词)
     */
    List<RetrievalResult> hybridSearch(Long knowledgeBaseId, String queryText, int topK);

    /**
     * 重排序检索结果
     */
    List<RetrievalResult> rerank(List<RetrievalResult> results, String queryText);

    /**
     * 获取检索配置
     */
    RetrievalConfig getRetrievalConfig(Long knowledgeBaseId);

    /**
     * 检索配置
     */
    record RetrievalConfig(
            int topK,
            double similarityThreshold,
            double rerankScoreThreshold
    ) {}
}
