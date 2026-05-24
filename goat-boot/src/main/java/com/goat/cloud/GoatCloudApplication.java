package com.goat.cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author wangjubin
 */
@SpringBootApplication(scanBasePackages = "com.goat.cloud")
@MapperScan("com.goat.cloud.module.system.mapper")
public class GoatCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoatCloudApplication.class, args);
    }
}
