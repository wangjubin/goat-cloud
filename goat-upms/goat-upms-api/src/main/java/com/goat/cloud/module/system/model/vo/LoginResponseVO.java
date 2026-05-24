package com.goat.cloud.module.system.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
@Builder
public class LoginResponseVO {

    private String accessToken;
    private String refreshToken;
    private ProfileVO profile;
}
