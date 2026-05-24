package com.goat.cloud.module.ai.service;

import com.goat.cloud.module.ai.entity.AiDocumentChunk;

import java.util.List;

/**
 * 向量存储服务接口
 * @author wangjubin
 */
public interface AiVectorStoreService {

    /**
     * 存储向量
     */
    void storeVector(AiDocumentChunk chunk, float[] embedding);

    /**
     * 批量存储向量
     */
    void storeVectors(List<AiDocumentChunk> chunks, List<float[]> embeddings);

    /**
     * 删除向量
     */
    void deleteVector(Long chunkId);

    /**
     * 删除知识库所有向量
     */
    void deleteVectorsByKnowledgeBase(Long knowledgeBaseId);

    /**
     * 更新向量
     */
    void updateVector(AiDocumentChunk chunk, float[] embedding);

    /**
     * 检查向量是否存在
     */
    boolean vectorExists(Long chunkId);
}
