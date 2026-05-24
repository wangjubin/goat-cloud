package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class RagSearchRequest {

    private String query;
    private Long knowledgeBaseId;
    private List<Long> knowledgeBaseIds;
    private Long documentId;
    private Integer topK;
    private Boolean includeContent;
    private Map<String, Object> options;
}
