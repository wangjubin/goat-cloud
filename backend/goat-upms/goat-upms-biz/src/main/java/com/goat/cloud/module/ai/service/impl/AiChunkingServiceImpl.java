package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.service.AiChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<ChunkResult> chunkDocument(AiDocument document, String content, ChunkingStrategy strategy) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        return switch (strategy) {
            case FIXED_SIZE -> chunkByFixedSize(content, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
            case BY_SENTENCE -> chunkBySentence(content);
            case BY_PARAGRAPH -> chunkByParagraph(content);
            case SEMANTIC -> chunkByFixedSize(content, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
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
}
