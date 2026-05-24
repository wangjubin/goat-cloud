package com.goat.cloud.module.system.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class ResetPasswordRequest {

    @NotNull
    private Long userId;
}
