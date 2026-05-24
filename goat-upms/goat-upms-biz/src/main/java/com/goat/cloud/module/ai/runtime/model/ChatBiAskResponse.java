package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class ChatBiAskResponse {

    private String question;
    private Long datasetId;
    private String datasetCode;
    private String datasetName;
    private Long datasourceId;
    private String datasourceName;
    private String candidateSql;
    private Boolean executable;
    private String executionPolicy;
    private List<String> safetyWarnings;
    private List<ChatBiTermMatch> matchedTerms;
    private List<ChatBiTableSnapshot> tables;
    private String explanation;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
