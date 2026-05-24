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
@TableName("ai_report_template")
@EqualsAndHashCode(callSuper = true)
public class AiReportTemplate extends BaseEntity {

    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;
    private String templateCode;
    private String templateName;
    private String description;
    private String chartType;
    private String templateJson;
    private String defaultOptions;
    private String dataMappingJson;
    private String status;
}
