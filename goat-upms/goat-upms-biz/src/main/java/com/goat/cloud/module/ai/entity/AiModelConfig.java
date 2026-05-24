package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import com.goat.cloud.common.enums.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_model_config")
@EqualsAndHashCode(callSuper = true)
public class AiModelConfig extends BaseEntity {

    @TableId(value = "model_id", type = IdType.AUTO)
    private Long modelId;
    private String modelName;
    private String provider;
    private String modelCode;
    private String modelType;
    private String endpoint;
    private String apiKeyRef;
    private String apiKeyEncrypted;
    private String apiKeyVersion;
    private String capabilityTags;
    private Integer contextWindow;
    private Boolean defaultModel;
    private CommonStatus status;
    private Integer sortOrder;
    private String remark;
}
