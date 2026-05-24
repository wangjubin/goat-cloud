package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_python_config")
@EqualsAndHashCode(callSuper = true)
public class AiPythonConfig extends BaseEntity {

    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;
    private String configCode;
    private String configName;
    private String executionMode;
    private String dockerImage;
    private String workingDir;
    private String envVarsJson;
    private String dependenciesJson;
    private Integer timeoutSeconds;
    private Integer memoryLimitMb;
    private String status;
}
