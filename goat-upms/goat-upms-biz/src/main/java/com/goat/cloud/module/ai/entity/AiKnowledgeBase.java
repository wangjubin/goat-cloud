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
@TableName("ai_knowledge_base")
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeBase extends BaseEntity {

    @TableId(value = "knowledge_base_id", type = IdType.AUTO)
    private Long knowledgeBaseId;
    private String knowledgeBaseCode;
    private String knowledgeBaseName;
    private String description;
    private Long vectorConfigId;
    private String embeddingModel;
    private Integer embeddingDimension;
    private Long documentCount;
    private Long chunkCount;
    private String retrievalConfig;
    private CommonStatus status;
    private String remark;
}
