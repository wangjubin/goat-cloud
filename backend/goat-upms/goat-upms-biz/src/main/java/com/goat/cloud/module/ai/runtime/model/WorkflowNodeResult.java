package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class WorkflowNodeResult {

    private Long nodeId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private String status;
    private Map<String, Object> output;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}