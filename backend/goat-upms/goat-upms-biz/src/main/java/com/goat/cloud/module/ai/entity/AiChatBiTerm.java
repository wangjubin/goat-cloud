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
@TableName("ai_chatbi_term")
@EqualsAndHashCode(callSuper = true)
public class AiChatBiTerm extends BaseEntity {

    @TableId(value = "term_id", type = IdType.AUTO)
    private Long termId;
    private String termCode;
    private String termName;
    private String synonyms;
    private String definition;
    private String expression;
    private Long datasetId;
    private CommonStatus status;
    private String remark;
}
