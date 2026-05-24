package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class ChatBiAskRequest {

    private String question;
    private Long datasourceId;
    private Long datasetId;
    private String datasetCode;
    private Integer limit;
    private Map<String, Object> options;
}
