package com.goat.cloud.module.ai.service;

/**
 * 文档处理服务接口
 * @author wangjubin
 */
public interface AiDocumentService {

    /**
     * 上传并解析文档
     */
    UploadResult uploadAndParse(Long knowledgeBaseId, String fileName, byte[] fileContent);

    /**
     * 文档上传结果
     */
    record UploadResult(
            Long documentId,
            String documentName,
            String parseStatus,
            int chunkCount,
            String message
    ) {}
}
