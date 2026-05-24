package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class WorkflowNodeTrace {

    private String nodeId;
    private String nodeType;
    private String nodeName;
    private String status;
    private Object input;
    private Object output;
    private Long durationMs;
}
