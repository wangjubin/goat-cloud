package com.goat.cloud.module.system.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
