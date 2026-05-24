package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_intent_config")
@EqualsAndHashCode(callSuper = true)
public class AiIntentConfig extends BaseEntity {

    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;
    private String intentCode;
    private String intentName;
    private String description;
    private String promptTemplate;
    private String examplesJson;
    private String modelConfigJson;
    private BigDecimal thresholdScore;
    private String fallbackIntent;
    private String status;
}
