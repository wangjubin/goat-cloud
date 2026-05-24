package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_billing_record")
@EqualsAndHashCode(callSuper = true)
public class AiBillingRecord extends BaseEntity {

    @TableId(value = "billing_id", type = IdType.AUTO)
    private Long billingId;
    private String conversationId;
    private String provider;
    private String modelCode;
    private String bizType;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal costAmount;
    private String currency;
    private LocalDateTime requestTime;
    private String status;
    private String remark;
}
