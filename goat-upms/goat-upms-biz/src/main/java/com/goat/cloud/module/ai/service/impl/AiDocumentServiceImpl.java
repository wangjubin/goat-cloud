package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.entity.AiDocumentChunk;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentMapper;
import com.goat.cloud.module.ai.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理服务实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiDocumentServiceImpl implements AiDocumentService {

    @Resource
    private AiDocumentMapper documentMapper;

    @Resource
    private AiDocumentChunkMapper chunkMapper;

    @Resource
    private AiDocumentParser documentParser;

    @Resource
    private AiChunkingService chunkingService;

    @Resource
    private AiEmbeddingService embeddingService;

    @Resource
    private AiVectorStoreService vectorStoreService;

    @Override
    @Transactional
    public UploadResult uploadAndParse(Long knowledgeBaseId, String fileName, byte[] fileContent) {
        try {
            AiDocument document = new AiDocument();
            document.setKnowledgeBaseId(knowledgeBaseId);
            document.setDocumentName(fileName);
            document.setFileSize((long) fileContent.length);
            document.setParseStatus("PROCESSING");
            document.setChunkStatus("PENDING");

            AiDocumentParser.DocumentType docType = documentParser.detectDocumentType(fileName, fileContent);
            document.setDocumentType(docType.name());

            documentMapper.insert(document);

            String content = documentParser.parseContent(document, fileContent);

            document.setParseStatus("SUCCESS");
            documentMapper.updateById(document);

            List<AiChunkingService.ChunkResult> chunks = chunkingService.chunkDocument(
                    document, content, AiChunkingService.ChunkingStrategy.BY_PARAGRAPH);

            List<AiDocumentChunk> chunkEntities = new ArrayList<>();
            List<float[]> embeddings = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                AiChunkingService.ChunkResult chunkResult = chunks.get(i);

                AiDocumentChunk chunk = new AiDocumentChunk();
                chunk.setKnowledgeBaseId(knowledgeBaseId);
                chunk.setDocumentId(document.getDocumentId());
                chunk.setChunkIndex(i);
                chunk.setTitle(chunkResult.title());
                chunk.setContent(chunkResult.content());
                chunk.setTokenCount(chunkResult.tokenCount());
                chunk.setEmbeddingStatus("PENDING");

                chunkMapper.insert(chunk);
                chunkEntities.add(chunk);

                float[] embedding = embeddingService.generateEmbedding(chunkResult.content());
                embeddings.add(embedding);
            }

            vectorStoreService.storeVectors(chunkEntities, embeddings);

            for (AiDocumentChunk chunk : chunkEntities) {
                chunk.setEmbeddingStatus("INDEXED");
                chunkMapper.updateById(chunk);
            }

            document.setChunkStatus("COMPLETED");
            documentMapper.updateById(document);

            log.info("Document uploaded and parsed: id={}, chunks={}", document.getDocumentId(), chunks.size());

            return new UploadResult(
                    document.getDocumentId(),
                    document.getDocumentName(),
                    document.getParseStatus(),
                    chunks.size(),
                    "文档上传并解析成功"
            );

        } catch (Exception e) {
            log.error("Failed to upload and parse document", e);
            return new UploadResult(
                    null,
                    fileName,
                    "FAILED",
                    0,
                    "解析失败: " + e.getMessage()
            );
        }
    }
}
