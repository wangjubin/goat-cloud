package com.goat.cloud.module.ai.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class AiChatResponse {

    private String conversationId;
    private String provider;
    private String modelCode;
    private AiChatMessage message;
    private String finishReason;
    private Boolean mock;
    private AiTokenUsageVO usage;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
