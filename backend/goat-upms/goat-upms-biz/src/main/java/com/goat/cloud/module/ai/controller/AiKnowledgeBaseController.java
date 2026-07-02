package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiKnowledgeBase;
import com.goat.cloud.module.ai.mapper.AiKnowledgeBaseMapper;
import com.goat.cloud.module.ai.service.AiEmbeddingService;
import com.goat.cloud.module.ai.service.AiService;
import com.goat.cloud.module.ai.service.AiVectorSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/knowledge-bases")
public class AiKnowledgeBaseController extends BaseAiCrudController<AiKnowledgeBase> {

    private final AiVectorSearchService vectorSearchService;
    private final AiEmbeddingService embeddingService;

    public AiKnowledgeBaseController(AiService aiService, AiKnowledgeBaseMapper mapper,
                                     AiVectorSearchService vectorSearchService,
                                     AiEmbeddingService embeddingService) {
        super(aiService, mapper);
        this.vectorSearchService = vectorSearchService;
        this.embeddingService = embeddingService;
    }

    /**
     * 知识库检索测试
     */
    @PostMapping("/{id}/search")
    public ApiResponse<List<AiVectorSearchService.VectorSearchResult>> search(
            @PathVariable Long id,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        // 生成查询向量
        float[] queryVector = embeddingService.generateEmbedding(query);
        // 执行向量搜索
        List<AiVectorSearchService.VectorSearchResult> results = vectorSearchService.search(
                queryVector, topK, List.of(id));
        return ApiResponse.success(results);
    }

    /**
     * 获取知识库向量统计
     */
    @GetMapping("/{id}/stats")
    public ApiResponse<Map<String, Object>> getStats(@PathVariable Long id) {
        long vectorCount = vectorSearchService.countVectors(id);
        return ApiResponse.success(Map.of(
                "knowledgeBaseId", id,
                "vectorCount", vectorCount,
                "vectorTableExists", vectorSearchService.vectorTableExists()
        ));
    }
}
