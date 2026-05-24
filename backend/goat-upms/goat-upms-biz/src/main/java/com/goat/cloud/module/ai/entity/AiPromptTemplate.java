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
@TableName("ai_prompt_template")
@EqualsAndHashCode(callSuper = true)
public class AiPromptTemplate extends BaseEntity {

    @TableId(value = "prompt_id", type = IdType.AUTO)
    private Long promptId;
    private String promptCode;
    private String promptName;
    private String promptType;
    private String systemPrompt;
    private String userPrompt;
    private String variables;
    private String version;
    private CommonStatus status;
    private String remark;
}
