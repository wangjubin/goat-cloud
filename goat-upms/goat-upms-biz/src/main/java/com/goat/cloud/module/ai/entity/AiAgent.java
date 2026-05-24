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
@TableName("ai_agent")
@EqualsAndHashCode(callSuper = true)
public class AiAgent extends BaseEntity {

    @TableId(value = "agent_id", type = IdType.AUTO)
    private Long agentId;
    private String agentCode;
    private String agentName;
    private String description;
    private Long modelId;
    private Long promptId;
    private String toolIds;
    private String knowledgeBaseIds;
    private String memoryConfig;
    private CommonStatus status;
    private String remark;
}
