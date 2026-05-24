package com.goat.cloud.module.ai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangjubin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModuleOverviewVO {

    private String code;
    private String name;
    private String basePath;
    private String description;
    private Long count;
}
