package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class WorkflowRunResponse {

    private String runId;
    private Long workflowId;
    private String workflowCode;
    private String workflowName;
    private String description;
    private String version;
    private String conversationId;
    private String status;
    private String summary;
    private Object output;
    private Object finalOutput;
    private Boolean complete;
    private Map<String, Object> variables;
    private List<WorkflowNodeTrace> traces;
    private List<WorkflowNodeResult> nodeResults;
    private Map<String, Object> metadata;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
