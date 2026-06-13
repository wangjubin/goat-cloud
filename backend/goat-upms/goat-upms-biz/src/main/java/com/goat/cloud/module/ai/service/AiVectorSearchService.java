package com.goat.cloud.module.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量搜索服务
 * 使用 PostgreSQL pgvector 进行向量相似度搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiVectorSearchService {

    private static final String VECTOR_TABLE = "ai_document_vectors";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 向量相似度搜索
     *
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @param knowledgeBaseIds 知识库 ID 列表（可选过滤）
     * @return 搜索结果列表，每个结果包含 chunkId 和 score
     */
    public List<VectorSearchResult> search(float[] queryVector, int topK, List<Long> knowledgeBaseIds) {
        if (queryVector == null || queryVector.length == 0) {
            return List.of();
        }

        String vectorJson = arrayToPgVector(queryVector);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT chunk_id, knowledge_base_id, document_id, ")
           .append("1 - (vector::jsonb::vector <=> ?::vector) AS score ")
           .append("FROM ").append(VECTOR_TABLE).append(" ");

        List<Object> params = new ArrayList<>();
        params.add(vectorJson);

        if (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty()) {
            sql.append("WHERE knowledge_base_id IN (");
            for (int i = 0; i < knowledgeBaseIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
                params.add(knowledgeBaseIds.get(i));
            }
            sql.append(") ");
        }

        sql.append("ORDER BY score DESC ");
        sql.append("LIMIT ?");
        params.add(topK);

        try {
            return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                VectorSearchResult result = new VectorSearchResult();
                result.setChunkId(rs.getLong("chunk_id"));
                result.setKnowledgeBaseId(rs.getLong("knowledge_base_id"));
                result.setDocumentId(rs.getLong("document_id"));
                result.setScore(rs.getDouble("score"));
                return result;
            });
        } catch (Exception e) {
            log.warn("Vector search failed, falling back to empty results: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取向量总数
     */
    public long countVectors(Long knowledgeBaseId) {
        String sql = "SELECT COUNT(*) FROM " + VECTOR_TABLE;
        if (knowledgeBaseId != null) {
            sql += " WHERE knowledge_base_id = ?";
            return jdbcTemplate.queryForObject(sql, Long.class, knowledgeBaseId);
        }
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * 检查向量表是否存在
     */
    public boolean vectorTableExists() {
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + VECTOR_TABLE, Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String arrayToPgVector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 向量搜索结果
     */
    @lombok.Data
    public static class VectorSearchResult {
        private Long chunkId;
        private Long knowledgeBaseId;
        private Long documentId;
        private double score;
    }
}
