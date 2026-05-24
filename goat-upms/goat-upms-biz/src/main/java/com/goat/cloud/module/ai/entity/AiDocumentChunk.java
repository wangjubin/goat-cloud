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
@TableName("ai_document_chunk")
@EqualsAndHashCode(callSuper = true)
public class AiDocumentChunk extends BaseEntity {

    @TableId(value = "chunk_id", type = IdType.AUTO)
    private Long chunkId;
    private Long knowledgeBaseId;
    private Long documentId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private Integer tokenCount;
    private String embeddingStatus;
    private String embeddingVector;
    private String metadata;
    private CommonStatus status;
    private String remark;
}
