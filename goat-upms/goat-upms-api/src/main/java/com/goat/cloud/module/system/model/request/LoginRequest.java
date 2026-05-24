package com.goat.cloud.module.system.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
