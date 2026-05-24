package com.goat.cloud.module.ai.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class AiIdsRequest {

    @NotEmpty
    private List<Long> ids;
}
