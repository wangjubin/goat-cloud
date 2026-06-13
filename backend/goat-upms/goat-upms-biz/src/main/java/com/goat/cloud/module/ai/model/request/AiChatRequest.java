package com.goat.cloud.module.ai.model.request;

import com.goat.cloud.module.ai.model.vo.AiChatMessage;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class AiChatRequest {

    private String conversationId;
    private Long modelId;
    private String provider;
    private String modelCode;
    private String message;
    private String systemPrompt;
    private List<AiChatMessage> messages;
    private BigDecimal temperature;
    private Boolean stream;
    private Map<String, Object> options;
    private String apiKeyRef;
}
