package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiDocumentChunk;
import com.goat.cloud.module.ai.entity.AiKnowledgeBase;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.mapper.AiKnowledgeBaseMapper;
import com.goat.cloud.module.ai.service.AiEmbeddingService;
import com.goat.cloud.module.ai.service.AiRetrievalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索服务实现 - RAG检索
 * @author wangjubin
 */
@Slf4j
@Service
public class AiRetrievalServiceImpl implements AiRetrievalService {

    private static final String VECTOR_TABLE = "ai_document_vectors";
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final int DEFAULT_TOP_K = 5;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AiDocumentChunkMapper chunkMapper;

    @Resource
    private AiKnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private AiEmbeddingService embeddingService;

    @Override
    public List<RetrievalResult> vectorSearch(Long knowledgeBaseId, float[] queryEmbedding, int topK) {
        String vectorStr = arrayToPgVector(queryEmbedding);
        String sql = """
            SELECT c.*, (v.vector <=> ?::vector) AS similarity
            FROM %s v
            JOIN ai_document_chunk c ON v.chunk_id = c.chunk_id
            WHERE v.knowledge_base_id = ?
            AND c.status = 'ENABLED'
            ORDER BY v.vector <=> ?::vector
            LIMIT ?
            """.formatted(VECTOR_TABLE);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorStr, knowledgeBaseId, vectorStr, topK);

        List<RetrievalResult> results = new ArrayList<>();
        int rank = 1;

        for (Map<String, Object> row : rows) {
            double similarity = ((Number) row.get("similarity")).doubleValue();
            if (similarity > 1.0) {
                similarity = 1.0 - similarity;
            }

            AiDocumentChunk chunk = mapToChunk(row);
            results.add(new RetrievalResult(chunk, similarity, rank++));
        }

        return results;
    }

    @Override
    public List<RetrievalResult> hybridSearch(Long knowledgeBaseId, String queryText, int topK) {
        float[] queryEmbedding = embeddingService.generateEmbedding(queryText);
        List<RetrievalResult> vectorResults = vectorSearch(knowledgeBaseId, queryEmbedding, topK * 2);

        String keywordSql = """
            SELECT * FROM ai_document_chunk
            WHERE knowledge_base_id = ?
            AND status = 'ENABLED'
            AND (content ILIKE ? OR title ILIKE ?)
            LIMIT ?
            """;

        String keywordPattern = "%" + queryText + "%";
        List<AiDocumentChunk> keywordChunks = jdbcTemplate.query(keywordSql,
                (rs, rowNum) -> {
                    AiDocumentChunk chunk = new AiDocumentChunk();
                    chunk.setChunkId(rs.getLong("chunk_id"));
                    chunk.setKnowledgeBaseId(rs.getLong("knowledge_base_id"));
                    chunk.setDocumentId(rs.getLong("document_id"));
                    chunk.setChunkIndex(rs.getInt("chunk_index"));
                    chunk.setTitle(rs.getString("title"));
                    chunk.setContent(rs.getString("content"));
                    chunk.setTokenCount(rs.getInt("token_count"));
                    chunk.setEmbeddingStatus(rs.getString("embedding_status"));
                    return chunk;
                },
                knowledgeBaseId, keywordPattern, keywordPattern, topK);

        Map<Long, RetrievalResult> resultMap = new LinkedHashMap<>();

        for (RetrievalResult result : vectorResults) {
            resultMap.put(result.chunk().getChunkId(), result);
        }

        for (AiDocumentChunk chunk : keywordChunks) {
            if (!resultMap.containsKey(chunk.getChunkId())) {
                resultMap.put(chunk.getChunkId(), new RetrievalResult(chunk, 0.5, resultMap.size() + 1));
            } else {
                RetrievalResult existing = resultMap.get(chunk.getChunkId());
                resultMap.put(chunk.getChunkId(),
                        new RetrievalResult(chunk, Math.min(1.0, existing.score() + 0.3), existing.rank()));
            }
        }

        return resultMap.values().stream()
                .sorted(Comparator.comparingDouble(RetrievalResult::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public List<RetrievalResult> rerank(List<RetrievalResult> results, String queryText) {
        if (results == null || results.isEmpty()) {
            return results;
        }

        List<RetrievalResult> reranked = results.stream()
                .sorted((a, b) -> {
                    double scoreA = calculateRerankScore(a, queryText);
                    double scoreB = calculateRerankScore(b, queryText);
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());

        List<RetrievalResult> resultWithRank = new ArrayList<>();
        for (int i = 0; i < reranked.size(); i++) {
            RetrievalResult r = reranked.get(i);
            resultWithRank.add(new RetrievalResult(r.chunk(), r.score(), i + 1));
        }

        return resultWithRank;
    }

    @Override
    public RetrievalConfig getRetrievalConfig(Long knowledgeBaseId) {
        AiKnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb != null && kb.getRetrievalConfig() != null) {
            try {
                String config = kb.getRetrievalConfig();
                return parseRetrievalConfig(config);
            } catch (Exception e) {
                log.warn("Failed to parse retrieval config for kb: {}", knowledgeBaseId);
            }
        }
        return new RetrievalConfig(DEFAULT_TOP_K, DEFAULT_SIMILARITY_THRESHOLD, 0.5);
    }

    private double calculateRerankScore(RetrievalResult result, String queryText) {
        double baseScore = result.score();

        String content = result.chunk().getContent();
        String title = result.chunk().getTitle();

        double titleBoost = 0.0;
        if (title != null && title.toLowerCase().contains(queryText.toLowerCase())) {
            titleBoost = 0.2;
        }

        double contentBoost = 0.0;
        if (content != null) {
            String lowerContent = content.toLowerCase();
            String lowerQuery = queryText.toLowerCase();
            long matches = lowerContent.split(lowerQuery, -1).length - 1;
            contentBoost = Math.min(0.3, matches * 0.05);
        }

        return baseScore + titleBoost + contentBoost;
    }

    private RetrievalConfig parseRetrievalConfig(String config) {
        int topK = DEFAULT_TOP_K;
        double threshold = DEFAULT_SIMILARITY_THRESHOLD;
        double rerankThreshold = 0.5;

        try {
            String[] parts = config.split(";");
            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv.length == 2) {
                    switch (kv[0].trim()) {
                        case "topK" -> topK = Integer.parseInt(kv[1].trim());
                        case "threshold" -> threshold = Double.parseDouble(kv[1].trim());
                        case "rerankThreshold" -> rerankThreshold = Double.parseDouble(kv[1].trim());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse retrieval config: {}", config);
        }

        return new RetrievalConfig(topK, threshold, rerankThreshold);
    }

    private AiDocumentChunk mapToChunk(Map<String, Object> row) {
        AiDocumentChunk chunk = new AiDocumentChunk();
        chunk.setChunkId(((Number) row.get("chunk_id")).longValue());
        chunk.setKnowledgeBaseId(((Number) row.get("knowledge_base_id")).longValue());
        chunk.setDocumentId(((Number) row.get("document_id")).longValue());
        chunk.setChunkIndex(((Number) row.get("chunk_index")).intValue());
        chunk.setTitle((String) row.get("title"));
        chunk.setContent((String) row.get("content"));
        chunk.setTokenCount(row.get("token_count") != null ? ((Number) row.get("token_count")).intValue() : 0);
        chunk.setEmbeddingStatus((String) row.get("embedding_status"));
        return chunk;
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
