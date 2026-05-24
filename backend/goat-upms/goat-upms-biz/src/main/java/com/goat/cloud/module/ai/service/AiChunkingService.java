package com.goat.cloud.module.ai.service;

import com.goat.cloud.module.ai.entity.AiDocument;

import java.util.List;

/**
 * 文档切片服务接口
 * @author wangjubin
 */
public interface AiChunkingService {

    /**
     * 切片策略类型
     */
    enum ChunkingStrategy {
        FIXED_SIZE,     // 固定大小切片
        BY_SENTENCE,    // 按句子切片
        BY_PARAGRAPH,   // 按段落切片
        SEMANTIC        // 语义切片
    }

    /**
     * 对文档内容进行切片
     */
    List<ChunkResult> chunkDocument(AiDocument document, String content, ChunkingStrategy strategy);

    /**
     * 计算文本的token数量(估算)
     */
    int estimateTokenCount(String text);

    /**
     * 切片结果
     */
    record ChunkResult(
            int chunkIndex,
            String title,
            String content,
            int tokenCount,
            int startPosition,
            int endPosition
    ) {}
}
