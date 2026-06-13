package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.service.AiChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档切片服务实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiChunkingServiceImpl implements AiChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[。！？.!?]+");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\n\\s*\n");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);

    @Override
    public List<ChunkResult> chunkDocument(AiDocument document, String content, ChunkingStrategy strategy) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        return switch (strategy) {
            case FIXED_SIZE -> chunkByFixedSize(content, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
            case BY_SENTENCE -> chunkBySentence(content);
            case BY_PARAGRAPH -> chunkByParagraph(content);
            case SEMANTIC -> chunkBySemantic(content);
        };
    }

    @Override
    public int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    private List<ChunkResult> chunkByFixedSize(String content, int chunkSize, int overlap) {
        List<ChunkResult> chunks = new ArrayList<>();
        int length = content.length();

        if (length <= chunkSize) {
            chunks.add(new ChunkResult(0, "chunk-0", content, estimateTokenCount(content), 0, length));
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            if (end < length && start + chunkSize < length) {
                int nearestSpace = content.lastIndexOf(' ', end);
                if (nearestSpace > start + chunkSize / 2) {
                    end = nearestSpace;
                }
            }

            String chunkContent = content.substring(start, end).trim();
            if (!chunkContent.isBlank()) {
                chunks.add(new ChunkResult(
                        chunkIndex,
                        "chunk-" + chunkIndex,
                        chunkContent,
                        estimateTokenCount(chunkContent),
                        start,
                        end
                ));
            }

            start = end - overlap;
            if (start >= length - 1) {
                break;
            }
            chunkIndex++;
        }

        return chunks;
    }

    private List<ChunkResult> chunkBySentence(String content) {
        List<ChunkResult> chunks = new ArrayList<>();
        Matcher matcher = SENTENCE_END_PATTERN.matcher(content);

        List<int[]> sentenceBoundaries = new ArrayList<>();
        int lastEnd = 0;

        while (matcher.find()) {
            int start = lastEnd;
            int end = matcher.end();
            String sentence = content.substring(start, end).trim();
            if (!sentence.isBlank()) {
                sentenceBoundaries.add(new int[]{start, end});
            }
            lastEnd = end;
        }

        if (lastEnd < content.length()) {
            String remaining = content.substring(lastEnd).trim();
            if (!remaining.isBlank()) {
                sentenceBoundaries.add(new int[]{lastEnd, content.length()});
            }
        }

        List<String> currentChunkSentences = new ArrayList<>();
        int currentChunkStart = 0;
        int currentTokenCount = 0;
        int chunkIndex = 0;

        for (int[] boundary : sentenceBoundaries) {
            String sentence = content.substring(boundary[0], boundary[1]).trim();
            int sentenceTokens = estimateTokenCount(sentence);

            if (currentTokenCount + sentenceTokens > DEFAULT_CHUNK_SIZE && !currentChunkSentences.isEmpty()) {
                String chunkContent = String.join("", currentChunkSentences);
                chunks.add(new ChunkResult(
                        chunkIndex,
                        "chunk-" + chunkIndex,
                        chunkContent,
                        currentTokenCount,
                        currentChunkStart,
                        currentChunkSentences.stream().mapToInt(String::length).sum() + currentChunkStart
                ));

                chunkIndex++;
                currentChunkSentences = new ArrayList<>();
                currentChunkStart = boundary[0];
                currentTokenCount = 0;
            }

            currentChunkSentences.add(sentence);
            currentTokenCount += sentenceTokens;
        }

        if (!currentChunkSentences.isEmpty()) {
            String chunkContent = String.join("", currentChunkSentences);
            chunks.add(new ChunkResult(
                    chunkIndex,
                    "chunk-" + chunkIndex,
                    chunkContent,
                    currentTokenCount,
                    currentChunkStart,
                    content.length()
            ));
        }

        return chunks;
    }

    private List<ChunkResult> chunkByParagraph(String content) {
        List<ChunkResult> chunks = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_PATTERN.split(content);

        List<String> currentChunkParagraphs = new ArrayList<>();
        int currentTokenCount = 0;
        int chunkIndex = 0;
        int currentChunkStart = 0;
        int lastEnd = 0;

        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }

            String trimmedParagraph = paragraph.trim();
            int paragraphTokens = estimateTokenCount(trimmedParagraph);

            if (currentTokenCount + paragraphTokens > DEFAULT_CHUNK_SIZE && !currentChunkParagraphs.isEmpty()) {
                String chunkContent = String.join("\n\n", currentChunkParagraphs);
                chunks.add(new ChunkResult(
                        chunkIndex,
                        "chunk-" + chunkIndex,
                        chunkContent,
                        currentTokenCount,
                        currentChunkStart,
                        lastEnd
                ));

                chunkIndex++;
                currentChunkParagraphs = new ArrayList<>();
                currentChunkStart = lastEnd;
                currentTokenCount = 0;
            }

            currentChunkParagraphs.add(trimmedParagraph);
            currentTokenCount += paragraphTokens;
            lastEnd += paragraph.length() + 2;
        }

        if (!currentChunkParagraphs.isEmpty()) {
            String chunkContent = String.join("\n\n", currentChunkParagraphs);
            chunks.add(new ChunkResult(
                    chunkIndex,
                    "chunk-" + chunkIndex,
                    chunkContent,
                    currentTokenCount,
                    currentChunkStart,
                    content.length()
            ));
        }

        return chunks;
    }

    /**
     * 语义分块：基于标题检测 + 段落边界 + 句子边界
     * 优先按标题分块，然后按段落，最后按句子
     */
    private List<ChunkResult> chunkBySemantic(String content) {
        List<ChunkResult> chunks = new ArrayList<>();

        // 第一步：按标题分割
        String[] sections = content.split("(?=^#{1,6}\\s+)", Pattern.MULTILINE);

        int chunkIndex = 0;
        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isBlank()) continue;

            // 如果段落足够小，直接作为一个 chunk
            if (estimateTokenCount(trimmed) <= DEFAULT_CHUNK_SIZE) {
                chunks.add(new ChunkResult(
                        chunkIndex,
                        "chunk-" + chunkIndex,
                        trimmed,
                        estimateTokenCount(trimmed),
                        0,
                        trimmed.length()
                ));
                chunkIndex++;
                continue;
            }

            // 段落太大，按句子边界分割
            List<ChunkResult> sectionChunks = chunkBySentence(trimmed);
            for (ChunkResult sectionChunk : sectionChunks) {
                chunks.add(new ChunkResult(
                        chunkIndex,
                        "chunk-" + chunkIndex,
                        sectionChunk.content(),
                        sectionChunk.tokenCount(),
                        sectionChunk.startPosition(),
                        sectionChunk.endPosition()
                ));
                chunkIndex++;
            }
        }

        // 如果没有检测到标题，回退到段落分块
        if (chunks.size() <= 1) {
            return chunkByParagraph(content);
        }

        return chunks;
    }

    /**
     * 对切片结果进行内容去重
     * 使用 SHA-256 哈希去除重复内容的切片
     */
    public List<ChunkResult> deduplicateChunks(List<ChunkResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return chunks;
        }

        Map<String, ChunkResult> seen = new LinkedHashMap<>();
        List<ChunkResult> deduplicated = new ArrayList<>();

        for (ChunkResult chunk : chunks) {
            String hash = sha256Hex(chunk.content());
            if (!seen.containsKey(hash)) {
                seen.put(hash, chunk);
                deduplicated.add(chunk);
            }
        }

        int removed = chunks.size() - deduplicated.size();
        if (removed > 0) {
            log.debug("Deduplicated {} duplicate chunks", removed);
        }

        return deduplicated;
    }

    /**
     * 计算内容的 SHA-256 哈希
     */
    public static String sha256Hex(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }
}
