package com.goat.cloud.module.ai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangjubin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTokenUsageVO {

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
