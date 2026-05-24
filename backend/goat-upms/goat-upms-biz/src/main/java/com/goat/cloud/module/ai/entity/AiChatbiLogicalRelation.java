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
@TableName("ai_chatbi_logical_relation")
@EqualsAndHashCode(callSuper = true)
public class AiChatbiLogicalRelation extends BaseEntity {

    @TableId(value = "relation_id", type = IdType.AUTO)
    private Long relationId;
    private Long datasetId;
    private String leftTable;
    private String leftColumn;
    private String rightTable;
    private String rightColumn;
    private String relationType;
    private String description;
}
