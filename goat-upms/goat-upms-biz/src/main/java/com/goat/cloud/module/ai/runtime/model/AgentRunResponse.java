package com.goat.cloud.module.ai.runtime.model;

import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class AgentRunResponse {

    private Long agentId;
    private String agentCode;
    private String agentName;
    private String description;
    private Long modelId;
    private Long promptId;
    private List<Long> knowledgeBaseIds;
    private List<Map<String, Object>> tools;
    private AiChatResponse chat;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
