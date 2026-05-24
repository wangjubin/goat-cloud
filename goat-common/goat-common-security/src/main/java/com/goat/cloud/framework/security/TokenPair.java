package com.goat.cloud.framework.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
@AllArgsConstructor
public class TokenPair {

    private String accessToken;
    private String refreshToken;
}
