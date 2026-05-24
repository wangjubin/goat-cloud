package com.goat.cloud.module.ai.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class AiOverviewVO {

    private String status;
    private String vectorStore;
    private List<String> capabilities;
    private List<AiModuleOverviewVO> modules;
}
