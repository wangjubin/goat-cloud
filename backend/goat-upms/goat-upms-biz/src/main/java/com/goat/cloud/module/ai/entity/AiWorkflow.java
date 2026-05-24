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
@TableName("ai_workflow")
@EqualsAndHashCode(callSuper = true)
public class AiWorkflow extends BaseEntity {

    @TableId(value = "workflow_id", type = IdType.AUTO)
    private Long workflowId;
    private String workflowCode;
    private String workflowName;
    private String description;
    private String triggerType;
    private String graphJson;
    private String version;
    private CommonStatus status;
    private String remark;
}
