package com.goat.cloud.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wangjubin
 */
@Data
@ConfigurationProperties(prefix = "goat.security")
public class SecurityProperties {

    private String jwtSecret = "change-me-in-production-change-me-in-production";
    private long accessTokenMinutes = 30;
    private long refreshTokenDays = 7;
    private String tokenPrefix = "Bearer ";
    private String headerName = "Authorization";
    private String initialPassword = "Admin@123456";
}
