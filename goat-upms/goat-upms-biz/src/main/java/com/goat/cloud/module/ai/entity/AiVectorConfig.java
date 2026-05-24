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
@TableName("ai_vector_config")
@EqualsAndHashCode(callSuper = true)
public class AiVectorConfig extends BaseEntity {

    @TableId(value = "vector_config_id", type = IdType.AUTO)
    private Long vectorConfigId;
    private String configName;
    private String provider;
    private String embeddingModel;
    private Integer embeddingDimension;
    private String distanceMetric;
    private String pgvectorTable;
    private String indexType;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private CommonStatus status;
    private String remark;
}
