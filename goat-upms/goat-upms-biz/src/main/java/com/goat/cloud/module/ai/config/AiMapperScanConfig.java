package com.goat.cloud.module.ai.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangjubin
 */
@Configuration
@MapperScan("com.goat.cloud.module.ai.mapper")
public class AiMapperScanConfig {
}
