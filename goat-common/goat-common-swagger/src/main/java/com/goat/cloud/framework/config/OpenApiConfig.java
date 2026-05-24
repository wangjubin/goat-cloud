package com.goat.cloud.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangjubin
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI goatOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Techen Cloud API")
                .version("1.0.0")
                .description("Techen Cloud base management platform API"));
    }
}
