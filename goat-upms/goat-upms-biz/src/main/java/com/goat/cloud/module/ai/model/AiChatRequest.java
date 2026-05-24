package com.goat.cloud.module.ai.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class AiChatRequest {

    @NotBlank
    private String message;

    private String conversationId;
}
