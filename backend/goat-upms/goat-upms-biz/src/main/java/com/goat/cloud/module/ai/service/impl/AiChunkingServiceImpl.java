package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.service.AiChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档切片服务实现
 * 支持多种切片策略：固定大小、按句子、按段落、语义切片
 * 
 * @author wangjubin
 */
@Slf4j
@Service
public class AiChunkingServiceImpl implements AiChunkingService {

    // 默认配置
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;
    
    // 句子分隔符正则
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[。！？.!?]+");
    
    // 段落分隔符正则
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");

    @Override
    public List<ChunkResult> chunkDocument(AiDocument document, String content, ChunkingStrategy strategy) {
        if (!StringUtils.hasText(content)) {
            log.warn("Document content is empty: documentId={}", document.getDocumentId());
            return List.of();
        }

        List<ChunkResult> chunks;
        
        switch (strategy) {
            case BY_SENTENCE:
                chunks = chunkBySentence(content);
                break;
            case BY_PARAGRAPH:
                chunks = chunkByParagraph(content);
                break;
            case SEMANTIC:
                chunks = chunkBySemantic(content);
                break;
            case FIXED_SIZE:
            default:
                chunks = chunkByFixedSize(content, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
                break;
        }
        
        log.info("Chunked document: documentId={}, strategy={}, chunkCount={}", 
                 document.getDocumentId(), strategy, chunks.size());
        
        return chunks;
    }

    @Override
    public int estimateTokenCount(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        // 简单估算：中文约1.5字符/token，英文约4字符/token
        // 这里使用保守估计：平均2字符/token
        return Math.max(1, text.length() / 2);
    }

    /**
     * 固定大小切片
     */
    private List<ChunkResult> chunkByFixedSize(String content, int chunkSize, int overlap) {
        List<ChunkResult> chunks = new ArrayList<>();
        int position = 0;
        int chunkIndex = 0;
        
        while (position < content.length()) {
            int end = Math.min(position + chunkSize, content.length());
            String chunkContent = content.substring(position, end);
            
            chunks.add(new ChunkResult(
                    chunkIndex,
                    String.format("片段 %d", chunkIndex + 1),
                    chunkContent,
                    estimateTokenCount(chunkContent),
                    position,
                    end
            ));
            
            position = end - overlap;
            chunkIndex++;
            
            // 防止无限循环
            if (position >= content.length() || chunkIndex > 10000) {
                break;
            }
        }
        
        return chunks;
    }

    /**
     * 按句子切片
     */
    private List<ChunkResult> chunkBySentence(String content) {
        List<ChunkResult> chunks = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(content);
        
        int lastEnd = 0;
        int chunkIndex = 0;
        StringBuilder currentChunk = new StringBuilder();
        int chunkStartPosition = 0;
        
        while (matcher.find()) {
            int sentenceEnd = matcher.end();
            String sentence = content.substring(lastEnd, sentenceEnd);
            
            currentChunk.append(sentence);
            
            // 如果当前块超过目标大小，保存并开始新块
            if (currentChunk.length() >= DEFAULT_CHUNK_SIZE) {
                String chunkContent = currentChunk.toString().trim();
                if (!chunkContent.isEmpty()) {
                    chunks.add(new ChunkResult(
                            chunkIndex,
                            String.format("片段 %d", chunkIndex + 1),
                            chunkContent,
                            estimateTokenCount(chunkContent),
                            chunkStartPosition,
                            sentenceEnd
                    ));
                    chunkIndex++;
                }
                
                currentChunk = new StringBuilder();
                chunkStartPosition = sentenceEnd;
            }
            
            lastEnd = sentenceEnd;
        }
        
        // 处理剩余内容
        if (currentChunk.length() > 0) {
            String chunkContent = currentChunk.toString().trim();
            if (!chunkContent.isEmpty()) {
                chunks.add(new ChunkResult(
                        chunkIndex,
                        String.format("片段 %d", chunkIndex + 1),
                        chunkContent,
                        estimateTokenCount(chunkContent),
                        chunkStartPosition,
                        content.length()
                ));
            }
        }
        
        return chunks;
    }

    /**
     * 按段落切片
     */
    private List<ChunkResult> chunkByParagraph(String content) {
        List<ChunkResult> chunks = new ArrayList<>();
        Matcher matcher = PARAGRAPH_PATTERN.matcher(content);
        
        int lastEnd = 0;
        int chunkIndex = 0;
        
        while (matcher.find()) {
            int paragraphEnd = matcher.start();
            String paragraph = content.substring(lastEnd, paragraphEnd).trim();
            
            if (!paragraph.isEmpty()) {
                // 如果段落太长，进一步分割
                if (paragraph.length() > DEFAULT_CHUNK_SIZE * 2) {
                    List<ChunkResult> subChunks = chunkByFixedSize(paragraph, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
                    for (ChunkResult subChunk : subChunks) {
                        chunks.add(new ChunkResult(
                                chunkIndex,
                                String.format("段落 %d - %s", chunkIndex + 1, subChunk.title()),
                                subChunk.content(),
                                subChunk.tokenCount(),
                                lastEnd + subChunk.startPosition(),
                                lastEnd + subChunk.endPosition()
                        ));
                        chunkIndex++;
                    }
                } else {
                    chunks.add(new ChunkResult(
                            chunkIndex,
                            String.format("段落 %d", chunkIndex + 1),
                            paragraph,
                            estimateTokenCount(paragraph),
                            lastEnd,
                            paragraphEnd
                    ));
                    chunkIndex++;
                }
            }
            
            lastEnd = matcher.end();
        }
        
        // 处理最后一个段落
        if (lastEnd < content.length()) {
            String lastParagraph = content.substring(lastEnd).trim();
            if (!lastParagraph.isEmpty()) {
                chunks.add(new ChunkResult(
                        chunkIndex,
                        String.format("段落 %d", chunkIndex + 1),
                        lastParagraph,
                        estimateTokenCount(lastParagraph),
                        lastEnd,
                        content.length()
                ));
            }
        }
        
        return chunks;
    }

    /**
     * 语义切片（简化版：基于标题和段落结构）
     */
    private List<ChunkResult> chunkBySemantic(String content) {
        List<ChunkResult> chunks = new ArrayList<>();
        
        // 简单实现：按标题分割（假设 # ## ### 等标记标题）
        Pattern headingPattern = Pattern.compile("^(#+)\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = headingPattern.matcher(content);
        
        int lastEnd = 0;
        int chunkIndex = 0;
        String lastTitle = "引言";
        
        while (matcher.find()) {
            int headingStart = matcher.start();
            
            // 保存前一个段落
            if (headingStart > lastEnd) {
                String section = content.substring(lastEnd, headingStart).trim();
                if (!section.isEmpty()) {
                    chunks.add(new ChunkResult(
                            chunkIndex,
                            lastTitle,
                            section,
                            estimateTokenCount(section),
                            lastEnd,
                            headingStart
                    ));
                    chunkIndex++;
                }
            }
            
            lastTitle = matcher.group(2);
            lastEnd = matcher.end();
        }
        
        // 处理最后一部分
        if (lastEnd < content.length()) {
            String lastSection = content.substring(lastEnd).trim();
            if (!lastSection.isEmpty()) {
                chunks.add(new ChunkResult(
                        chunkIndex,
                        lastTitle,
                        lastSection,
                        estimateTokenCount(lastSection),
                        lastEnd,
                        content.length()
                ));
            }
        }
        
        // 如果没有找到标题，回退到段落切片
        if (chunks.isEmpty()) {
            return chunkByParagraph(content);
        }
        
        return chunks;
    }
}
