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
@TableName("ai_api_skill")
@EqualsAndHashCode(callSuper = true)
public class AiApiSkill extends BaseEntity {

    @TableId(value = "api_skill_id", type = IdType.AUTO)
    private Long apiSkillId;
    private String skillCode;
    private String skillName;
    private String skillType;
    private String endpoint;
    private String httpMethod;
    private String authType;
    private String requestSchema;
    private String responseSchema;
    private CommonStatus status;
    private String remark;
}
