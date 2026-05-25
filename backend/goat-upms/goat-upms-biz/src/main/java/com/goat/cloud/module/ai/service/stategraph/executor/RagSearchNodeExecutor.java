package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.runtime.AiRagSearchService;
import com.goat.cloud.module.ai.runtime.model.RagSearchRequest;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * RAG 搜索节点执行器
 * <p>
 * 在工作流中执行知识库检索，获取与用户问题相关的文档切片
 * <p>
 * 节点配置示例：
 * {
 *   "knowledgeBaseId": 1,
 *   "topK": 5
 * }
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchNodeExecutor implements NodeExecutor {

    private final AiRagSearchService ragSearchService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "RAG_SEARCH";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            if (nodeConfig != null && !nodeConfig.isBlank()) {
                config = objectMapper.readValue(nodeConfig, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            }

            // Resolve topK: config first, then context fallback, then default 5
            Integer topK = toInteger(config.get("topK"));
            if (topK == null) {
                topK = toInteger(context.get("topK"));
            }
            if (topK == null) {
                topK = 5;
            }

            // Resolve knowledgeBaseId: config first, then context fallback
            Long knowledgeBaseId = toLong(config.get("knowledgeBaseId"));
            if (knowledgeBaseId == null) {
                knowledgeBaseId = toLong(context.get("knowledgeBaseId"));
            }

            // Resolve query: userMessage first, then query
            String query = asString(context.get("userMessage"));
            if (query.isBlank()) {
                query = asString(context.get("query"));
            }
            if (query.isBlank()) {
                return NodeResult.fail("RAG search requires a query (userMessage or query in context)");
            }

            RagSearchRequest ragRequest = new RagSearchRequest();
            ragRequest.setQuery(query);
            ragRequest.setTopK(topK);
            ragRequest.setKnowledgeBaseId(knowledgeBaseId);
            ragRequest.setIncludeContent(Boolean.TRUE);

            RagSearchResponse ragResponse = ragSearchService.search(ragRequest);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("hits", ragResponse.getHits());
            output.put("total", ragResponse.getTotal());
            output.put("query", ragResponse.getQuery());

            context.put("ragSearchResult", output);

            return NodeResult.ok(objectMapper.writeValueAsString(output));

        } catch (Exception e) {
            log.error("RAG search node execution error", e);
            return NodeResult.fail("RAG search failed: " + e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }
}