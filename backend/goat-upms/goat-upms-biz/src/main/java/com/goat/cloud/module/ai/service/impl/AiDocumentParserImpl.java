package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.service.AiDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 文档解析服务实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiDocumentParserImpl implements AiDocumentParser {

    private static final Pattern PDF_HEADER = Pattern.compile("%PDF");
    private static final Pattern DOCX_HEADER = Pattern.compile("PK\\x03\\x04");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    @Override
    public String parseContent(AiDocument document, byte[] fileContent) {
        if (fileContent == null || fileContent.length == 0) {
            return "";
        }

        DocumentType docType = detectDocumentType(document.getDocumentName(), fileContent);

        return switch (docType) {
            case TXT -> parseTxt(fileContent);
            case MARKDOWN -> parseMarkdown(fileContent);
            case HTML -> parseHtml(fileContent);
            case PDF -> parsePdf(fileContent);
            case DOCX -> parseDocx(fileContent);
            default -> parseTxt(fileContent);
        };
    }

    @Override
    public DocumentType detectDocumentType(String fileName, byte[] fileContent) {
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".txt")) {
                return DocumentType.TXT;
            }
            if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
                return DocumentType.MARKDOWN;
            }
            if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
                return DocumentType.HTML;
            }
            if (lowerName.endsWith(".pdf")) {
                return DocumentType.PDF;
            }
            if (lowerName.endsWith(".docx")) {
                return DocumentType.DOCX;
            }
        }

        if (fileContent != null && fileContent.length > 4) {
            String header = new String(fileContent, 0, Math.min(8, fileContent.length), StandardCharsets.US_ASCII);
            if (PDF_HEADER.matcher(header).find()) {
                return DocumentType.PDF;
            }
            if (DOCX_HEADER.matcher(header).find()) {
                return DocumentType.DOCX;
            }
        }

        return DocumentType.UNKNOWN;
    }

    @Override
    public DocumentMetadata extractMetadata(AiDocument document, byte[] fileContent) {
        String fileName = document.getDocumentName();
        DocumentType docType = detectDocumentType(fileName, fileContent);

        return new DocumentMetadata(
                fileName,
                docType.name().toLowerCase(),
                fileContent != null ? fileContent.length : 0,
                estimatePageCount(fileContent, docType),
                extractTitle(fileContent, docType),
                null
        );
    }

    private String parseTxt(byte[] content) {
        if (content == null) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String parseMarkdown(byte[] content) {
        String markdown = parseTxt(content);
        return stripMarkdownSyntax(markdown);
    }

    private String parseHtml(byte[] content) {
        String html = parseTxt(content);
        String text = HTML_TAG_PATTERN.matcher(html).replaceAll(" ");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private String parsePdf(byte[] content) {
        log.warn("PDF parsing uses basic text extraction. For full PDF support, add Apache PDFBox dependency.");
        return extractPdfText(content);
    }

    private String parseDocx(byte[] content) {
        log.warn("DOCX parsing uses basic text extraction. For full DOCX support, add Apache POI dependency.");
        return extractDocxText(content);
    }

    private String stripMarkdownSyntax(String markdown) {
        if (markdown == null) {
            return "";
        }
        String result = markdown;
        result = result.replaceAll("#+\\s*", "");
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        result = result.replaceAll("\\*(.+?)\\*", "$1");
        result = result.replaceAll("__(.+?)__", "$1");
        result = result.replaceAll("_(.+?)_", "$1");
        result = result.replaceAll("```[\\s\\S]*?```", "");
        result = result.replaceAll("`(.+?)`", "$1");
        result = result.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        result = result.replaceAll("!\\[([^\\]]*)\\]\\([^)]+\\)", "");
        return result.trim();
    }

    private String extractPdfText(byte[] content) {
        if (content == null) {
            return "";
        }
        String text = new String(content, StandardCharsets.UTF_8);
        text = text.replaceAll("[^\\x20-\\x7E\\u4E00-\\u9FFF]", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private String extractDocxText(byte[] content) {
        return extractPdfText(content);
    }

    private int estimatePageCount(byte[] content, DocumentType type) {
        if (content == null) {
            return 1;
        }
        int charCount = content.length;
        return switch (type) {
            case PDF -> Math.max(1, charCount / 1500);
            case DOCX -> Math.max(1, charCount / 500);
            default -> Math.max(1, charCount / 2000);
        };
    }

    private String extractTitle(byte[] content, DocumentType type) {
        if (content == null) {
            return null;
        }
        String text = new String(content, StandardCharsets.UTF_8);
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() < 200) {
                if (trimmed.startsWith("#")) {
                    return trimmed.replaceAll("#+\\s*", "");
                }
                if (type == DocumentType.TXT && trimmed.length() > 5) {
                    return trimmed;
                }
            }
        }
        return null;
    }
}
