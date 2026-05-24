package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class ChatBiTermMatch {

    private Long termId;
    private String termCode;
    private String termName;
    private String expression;
    private String definition;
    private Double score;
    private String reason;
}
