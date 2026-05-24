package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class RagSearchHit {

    private Long chunkId;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentName;
    private String documentType;
    private String sourceUri;
    private Integer chunkIndex;
    private String title;
    private String content;
    private String contentPreview;
    private Integer tokenCount;
    private Double score;
    private String citation;
    private Map<String, Object> metadata;
}
