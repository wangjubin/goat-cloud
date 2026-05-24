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
@TableName("ai_document")
@EqualsAndHashCode(callSuper = true)
public class AiDocument extends BaseEntity {

    @TableId(value = "document_id", type = IdType.AUTO)
    private Long documentId;
    private Long knowledgeBaseId;
    private String documentName;
    private String documentType;
    private String sourceUri;
    private Long fileSize;
    private String parseStatus;
    private String chunkStatus;
    private String metadata;
    private CommonStatus status;
    private String remark;
}
