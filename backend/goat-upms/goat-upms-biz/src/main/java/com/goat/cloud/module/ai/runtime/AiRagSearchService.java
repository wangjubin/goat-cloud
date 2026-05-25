package com.goat.cloud.module.ai.runtime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.entity.AiDocumentChunk;
import com.goat.cloud.module.ai.entity.AiKnowledgeBase;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentMapper;
import com.goat.cloud.module.ai.mapper.AiKnowledgeBaseMapper;
import com.goat.cloud.module.ai.runtime.model.RagSearchHit;
import com.goat.cloud.module.ai.runtime.model.RagSearchRequest;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRagSearchService {

    private static final int DEFAULT_RAG_TOP_K = 5;
    private static final int MAX_RAG_TOP_K = 20;
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s,，。.!！?？;；:：()（）\\[\\]{}<>《》\"'`]+");

    private final AiDocumentChunkMapper documentChunkMapper;
    private final AiDocumentMapper documentMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;

    public RagSearchResponse search(RagSearchRequest request) {
        RagSearchRequest safeRequest = request == null ? new RagSearchRequest() : request;
        String query = normalizeText(safeRequest.getQuery());
        int topK = clamp(safeRequest.getTopK(), DEFAULT_RAG_TOP_K, 1, MAX_RAG_TOP_K);
        List<String> terms = searchTerms(query);

        List<AiDocumentChunk> chunks = safeSelectList(documentChunkMapper, buildChunkQuery(safeRequest, terms, topK));
        Map<Long, AiDocument> documents = selectByIds(documentMapper, chunks.stream()
                .map(AiDocumentChunk::getDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, AiKnowledgeBase> knowledgeBases = selectByIds(knowledgeBaseMapper, chunks.stream()
                .map(AiDocumentChunk::getKnowledgeBaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        boolean includeContent = Boolean.TRUE.equals(safeRequest.getIncludeContent());
        List<RagSearchHit> hits = chunks.stream()
                .map(chunk -> toHit(chunk, documents.get(chunk.getDocumentId()), knowledgeBases.get(chunk.getKnowledgeBaseId()), query, terms, includeContent))
                .filter(hit -> !StringUtils.hasText(query) || hit.getScore() > 0)
                .sorted(Comparator.comparing(RagSearchHit::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RagSearchHit::getChunkId, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(topK)
                .toList();

        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setTotal(hits.size());
        response.setHits(hits);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topK", topK);
        metadata.put("searchMode", "postgres-keyword-plus-local-scoring");
        metadata.put("candidateCount", chunks.size());
        metadata.put("knowledgeBaseIds", effectiveKnowledgeBaseIds(safeRequest));
        metadata.put("documentId", safeRequest.getDocumentId());
        response.setMetadata(metadata);
        response.setSearchedAt(java.time.LocalDateTime.now());
        return response;
    }

    String buildRagContext(RagSearchResponse rag) {
        if (rag == null || rag.getHits() == null || rag.getHits().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rag.getHits().size(); i++) {
            RagSearchHit hit = rag.getHits().get(i);
            builder.append("[").append(i + 1).append("] ").append(hit.getCitation()).append("\n")
                    .append(firstText(hit.getContent(), hit.getContentPreview(), "")).append("\n");
        }
        return builder.toString();
    }

    private QueryWrapper<AiDocumentChunk> buildChunkQuery(RagSearchRequest request, List<String> terms, int topK) {
        QueryWrapper<AiDocumentChunk> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ENABLED");
        if (request.getDocumentId() != null) {
            wrapper.eq("document_id", request.getDocumentId());
        }
        List<Long> knowledgeBaseIds = effectiveKnowledgeBaseIds(request);
        if (!knowledgeBaseIds.isEmpty()) {
            wrapper.in("knowledge_base_id", knowledgeBaseIds);
        }
        if (StringUtils.hasText(request.getQuery())) {
            wrapper.and(nested -> {
                nested.like("title", request.getQuery()).or().like("content", request.getQuery());
                for (String term : terms) {
                    nested.or().like("title", term).or().like("content", term);
                }
            });
        }
        return wrapper.orderByDesc("create_time").last("limit " + Math.max(50, topK * 20));
    }

    private RagSearchHit toHit(AiDocumentChunk chunk, AiDocument document, AiKnowledgeBase knowledgeBase,
                               String query, List<String> terms, boolean includeContent) {
        RagSearchHit hit = new RagSearchHit();
        hit.setChunkId(chunk.getChunkId());
        hit.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        hit.setKnowledgeBaseName(knowledgeBase == null ? null : knowledgeBase.getKnowledgeBaseName());
        hit.setDocumentId(chunk.getDocumentId());
        hit.setDocumentName(document == null ? null : document.getDocumentName());
        hit.setDocumentType(document == null ? null : document.getDocumentType());
        hit.setSourceUri(document == null ? null : document.getSourceUri());
        hit.setChunkIndex(chunk.getChunkIndex());
        hit.setTitle(chunk.getTitle());
        hit.setContent(includeContent ? chunk.getContent() : null);
        hit.setContentPreview(preview(chunk.getContent(), 240));
        hit.setTokenCount(chunk.getTokenCount());
        hit.setScore(scoreChunk(chunk, query, terms));
        hit.setCitation(citation(knowledgeBase, document, chunk));
        hit.setMetadata(parseJson(chunk.getMetadata()));
        return hit;
    }

    private double scoreChunk(AiDocumentChunk chunk, String query, List<String> terms) {
        if (!StringUtils.hasText(query)) {
            return 0.1D;
        }
        String title = lower(chunk.getTitle());
        String content = lower(chunk.getContent());
        String normalizedQuery = lower(query);
        double score = 0D;
        if (title.contains(normalizedQuery)) {
            score += 8D;
        }
        if (content.contains(normalizedQuery)) {
            score += 6D;
        }
        for (String term : terms) {
            String normalizedTerm = lower(term);
            if (normalizedTerm.length() < 2) {
                continue;
            }
            if (title.contains(normalizedTerm)) {
                score += 3D;
            }
            score += countOccurrences(content, normalizedTerm);
        }
        if (StringUtils.hasText(chunk.getTitle())) {
            score += 0.2D;
        }
        return Math.round(score * 100D) / 100D;
    }

    private String citation(AiKnowledgeBase knowledgeBase, AiDocument document, AiDocumentChunk chunk) {
        return firstText(knowledgeBase == null ? null : knowledgeBase.getKnowledgeBaseName(), "KB#" + chunk.getKnowledgeBaseId())
                + " / "
                + firstText(document == null ? null : document.getDocumentName(), "DOC#" + chunk.getDocumentId())
                + " #chunk-" + firstText(chunk.getChunkIndex() == null ? null : String.valueOf(chunk.getChunkIndex()), String.valueOf(chunk.getChunkId()));
    }

    List<String> searchTerms(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String part : SPLIT_PATTERN.split(query.trim())) {
            if (StringUtils.hasText(part)) {
                terms.add(part.trim());
            }
        }
        if (terms.isEmpty()) {
            terms.add(query.trim());
        }
        return new ArrayList<>(terms);
    }

    private List<Long> effectiveKnowledgeBaseIds(RagSearchRequest request) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (request.getKnowledgeBaseId() != null) {
            ids.add(request.getKnowledgeBaseId());
        }
        if (request.getKnowledgeBaseIds() != null) {
            ids.addAll(request.getKnowledgeBaseIds().stream().filter(Objects::nonNull).toList());
        }
        return new ArrayList<>(ids);
    }

    // --- Shared utility helpers (public for cross-service use) ---

    static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    static String lower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    static String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    static String preview(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength) + "...";
    }

    static int clamp(Integer value, int defaultValue, int min, int max) {
        int safeValue = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, safeValue));
    }

    private int countOccurrences(String text, String term) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(term)) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) >= 0) {
            count++;
            index += term.length();
        }
        return count;
    }

    private Map<String, Object> parseJson(String text) {
        if (!StringUtils.hasText(text)) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = om.readTree(text);
            if (node.isObject()) {
                return om.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", om.convertValue(node, Object.class));
            return map;
        } catch (Exception ex) {
            return Map.of("raw", text);
        }
    }

    private <T> T safeSelectById(BaseMapper<T> mapper, java.io.Serializable id) {
        try {
            return mapper.selectById(id);
        } catch (RuntimeException ex) {
            if (AiRuntimeHelper.isMissingTable(ex)) {
                return null;
            }
            throw ex;
        }
    }

    private <T> List<T> safeSelectList(BaseMapper<T> mapper, QueryWrapper<T> wrapper) {
        try {
            return mapper.selectList(wrapper);
        } catch (RuntimeException ex) {
            if (AiRuntimeHelper.isMissingTable(ex)) {
                return List.of();
            }
            throw ex;
        }
    }

    private <T> Map<Long, T> selectByIds(BaseMapper<T> mapper, java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            return mapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(AiRuntimeHelper::entityId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        } catch (RuntimeException ex) {
            if (AiRuntimeHelper.isMissingTable(ex)) {
                return Map.of();
            }
            throw ex;
        }
    }
}