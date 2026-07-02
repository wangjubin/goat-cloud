package com.goat.cloud.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.entity.AiDocumentChunk;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentMapper;
import com.goat.cloud.module.ai.service.AiChunkingService;
import com.goat.cloud.module.ai.service.AiDocumentParser;
import com.goat.cloud.module.ai.service.AiDocumentService;
import com.goat.cloud.module.ai.service.AiEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档处理服务实现
 * 负责文档上传、解析、切片、向量化全流程
 * 
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDocumentServiceImpl implements AiDocumentService {

    private final AiDocumentMapper documentMapper;
    private final AiDocumentChunkMapper chunkMapper;
    private final AiDocumentParser documentParser;
    private final AiChunkingService chunkingService;
    private final AiEmbeddingService embeddingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResult uploadAndParse(Long knowledgeBaseId, String fileName, byte[] fileContent) {
        log.info("Starting document upload: knowledgeBaseId={}, fileName={}", knowledgeBaseId, fileName);
        
        try {
            // 1. 创建文档记录
            AiDocument document = new AiDocument();
            document.setKnowledgeBaseId(knowledgeBaseId);
            document.setDocumentName(fileName);
            document.setDocumentType(extractFileType(fileName));
            document.setFileSize((long) fileContent.length);
            document.setParseStatus("PROCESSING");
            document.setChunkStatus("PENDING");
            document.setStatus(CommonStatus.ENABLED);
            
            documentMapper.insert(document);
            Long documentId = document.getDocumentId();
            
            log.info("Created document record: documentId={}", documentId);
            
            // 2. 解析文档内容
            String content;
            try {
                content = documentParser.parseContent(document, fileContent);
                if (!StringUtils.hasText(content)) {
                    throw new RuntimeException("文档解析结果为空");
                }
                
                document.setParseStatus("SUCCESS");
                documentMapper.updateById(document);
                
                log.info("Document parsed successfully: documentId={}, contentLength={}", 
                         documentId, content.length());
                
            } catch (Exception e) {
                log.error("Document parse failed: documentId={}", documentId, e);
                document.setParseStatus("FAILED");
                document.setRemark("解析失败: " + e.getMessage());
                documentMapper.updateById(document);
                
                return new UploadResult(documentId, fileName, "FAILED", 0, 
                                       "文档解析失败: " + e.getMessage());
            }
            
            // 3. 文档切片
            document.setChunkStatus("PROCESSING");
            documentMapper.updateById(document);
            
            List<AiChunkingService.ChunkResult> chunks = chunkingService.chunkDocument(
                    document, content, AiChunkingService.ChunkingStrategy.BY_PARAGRAPH);
            
            if (chunks.isEmpty()) {
                throw new RuntimeException("文档切片结果为空");
            }
            
            log.info("Document chunked: documentId={}, chunkCount={}", documentId, chunks.size());
            
            // 4. 保存切片并生成向量
            int successCount = 0;
            for (AiChunkingService.ChunkResult chunkResult : chunks) {
                try {
                    // 生成向量
                    float[] vector = embeddingService.generateEmbedding(chunkResult.content());
                    
                    // 保存切片
                    AiDocumentChunk chunk = new AiDocumentChunk();
                    chunk.setDocumentId(documentId);
                    chunk.setKnowledgeBaseId(knowledgeBaseId);
                    chunk.setChunkIndex(chunkResult.chunkIndex());
                    chunk.setTitle(chunkResult.title());
                    chunk.setContent(chunkResult.content());
                    chunk.setTokenCount(chunkResult.tokenCount());
                    chunk.setStatus(CommonStatus.ENABLED);
                    
                    chunkMapper.insert(chunk);
                    
                    // TODO: 保存向量到向量数据库
                    // vectorStoreService.saveVector(chunk.getChunkId(), vector);
                    
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to process chunk: documentId={}, chunkIndex={}", 
                             documentId, chunkResult.chunkIndex(), e);
                }
            }
            
            // 5. 更新文档状态
            document.setChunkStatus("SUCCESS");
            document.setRemark(String.format("成功处理 %d/%d 个切片", successCount, chunks.size()));
            documentMapper.updateById(document);
            
            log.info("Document processing completed: documentId={}, successCount={}/{}", 
                     documentId, successCount, chunks.size());
            
            return new UploadResult(documentId, fileName, "SUCCESS", successCount, 
                                   String.format("文档处理成功，共生成 %d 个知识切片", successCount));
            
        } catch (Exception e) {
            log.error("Document upload and parse failed: knowledgeBaseId={}, fileName={}", 
                     knowledgeBaseId, fileName, e);
            return new UploadResult(null, fileName, "FAILED", 0, 
                                   "文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 提取文件类型
     */
    private String extractFileType(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "UNKNOWN";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
    }

    /**
     * 获取文档处理状态
     */
    public AiDocument getDocumentStatus(Long documentId) {
        return documentMapper.selectById(documentId);
    }

    /**
     * 获取知识库的文档列表
     */
    public List<AiDocument> getDocumentsByKnowledgeBase(Long knowledgeBaseId) {
        LambdaQueryWrapper<AiDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiDocument::getKnowledgeBaseId, knowledgeBaseId)
               .eq(AiDocument::getStatus, CommonStatus.ENABLED)
               .orderByDesc(AiDocument::getCreateTime);
        return documentMapper.selectList(wrapper);
    }

    /**
     * 删除文档及其切片
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        log.info("Deleting document: documentId={}", documentId);
        
        // 1. 删除文档切片
        LambdaQueryWrapper<AiDocumentChunk> chunkWrapper = new LambdaQueryWrapper<>();
        chunkWrapper.eq(AiDocumentChunk::getDocumentId, documentId);
        chunkMapper.delete(chunkWrapper);
        
        // 2. 删除文档记录
        documentMapper.deleteById(documentId);
        
        // TODO: 删除向量数据库中的向量
        // vectorStoreService.deleteVectorsByDocumentId(documentId);
        
        log.info("Document deleted: documentId={}", documentId);
    }
}
