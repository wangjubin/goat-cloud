package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiDocumentChunk;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.service.AiVectorStoreService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量存储服务实现 - pgvector
 * @author wangjubin
 */
@Slf4j
@Service
public class AiVectorStoreServiceImpl implements AiVectorStoreService {

    private static final String VECTOR_TABLE = "ai_document_vectors";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AiDocumentChunkMapper chunkMapper;

    @Override
    public void storeVector(AiDocumentChunk chunk, float[] embedding) {
        String vectorJson = arrayToPgVector(embedding);
        String sql = """
            INSERT INTO %s (chunk_id, knowledge_base_id, document_id, vector)
            VALUES (?, ?, ?, ?::jsonb)
            ON CONFLICT (chunk_id) DO UPDATE SET vector = ?::jsonb
            """.formatted(VECTOR_TABLE);

        jdbcTemplate.update(sql, chunk.getChunkId(), chunk.getKnowledgeBaseId(),
                chunk.getDocumentId(), vectorJson, vectorJson);

        chunk.setEmbeddingStatus("indexed");
        chunkMapper.updateById(chunk);
        log.debug("Stored vector for chunk: {}", chunk.getChunkId());
    }

    @Override
    public void storeVectors(List<AiDocumentChunk> chunks, List<float[]> embeddings) {
        if (chunks == null || embeddings == null || chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Chunks and embeddings must have same size");
        }

        for (int i = 0; i < chunks.size(); i++) {
            storeVector(chunks.get(i), embeddings.get(i));
        }
        log.info("Stored {} vectors batch", chunks.size());
    }

    @Override
    public void deleteVector(Long chunkId) {
        String sql = "DELETE FROM %s WHERE chunk_id = ?".formatted(VECTOR_TABLE);
        jdbcTemplate.update(sql, chunkId);

        AiDocumentChunk chunk = chunkMapper.selectById(chunkId);
        if (chunk != null) {
            chunk.setEmbeddingStatus("pending");
            chunkMapper.updateById(chunk);
        }
        log.debug("Deleted vector for chunk: {}", chunkId);
    }

    @Override
    public void deleteVectorsByKnowledgeBase(Long knowledgeBaseId) {
        String sql = "DELETE FROM %s WHERE knowledge_base_id = ?".formatted(VECTOR_TABLE);
        jdbcTemplate.update(sql, knowledgeBaseId);
        log.info("Deleted all vectors for knowledge base: {}", knowledgeBaseId);
    }

    @Override
    public void updateVector(AiDocumentChunk chunk, float[] embedding) {
        storeVector(chunk, embedding);
    }

    @Override
    public boolean vectorExists(Long chunkId) {
        String sql = "SELECT COUNT(*) FROM %s WHERE chunk_id = ?".formatted(VECTOR_TABLE);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, chunkId);
        return count != null && count > 0;
    }

    private String arrayToPgVector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
