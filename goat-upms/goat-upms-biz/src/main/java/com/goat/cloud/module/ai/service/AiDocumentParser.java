package com.goat.cloud.module.ai.service;

import com.goat.cloud.module.ai.entity.AiDocument;

/**
 * 文档解析服务接口
 * @author wangjubin
 */
public interface AiDocumentParser {

    /**
     * 支持的文档类型
     */
    enum DocumentType {
        PDF,
        DOCX,
        TXT,
        MARKDOWN,
        HTML,
        UNKNOWN
    }

    /**
     * 解析文档内容
     */
    String parseContent(AiDocument document, byte[] fileContent);

    /**
     * 检测文档类型
     */
    DocumentType detectDocumentType(String fileName, byte[] fileContent);

    /**
     * 提取文档元数据
     */
    DocumentMetadata extractMetadata(AiDocument document, byte[] fileContent);

    /**
     * 文档元数据
     */
    record DocumentMetadata(
            String fileName,
            String fileType,
            long fileSize,
            int pageCount,
            String title,
            String author
    ) {}
}
