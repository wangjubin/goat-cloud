package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class WorkflowRunRequest {

    private String message;
    private String conversationId;
    private Map<String, Object> input;
    private Map<String, Object> variables;
    private Map<String, Object> options;
}
