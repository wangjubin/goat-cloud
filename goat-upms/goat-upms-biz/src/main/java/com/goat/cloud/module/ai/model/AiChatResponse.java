package com.goat.cloud.module.ai.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
@Builder
public class AiChatResponse {

    private String conversationId;
    private String answer;
    private List<String> citations;
}
