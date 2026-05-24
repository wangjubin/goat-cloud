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
@TableName("ai_chatbi_dataset")
@EqualsAndHashCode(callSuper = true)
public class AiChatBiDataset extends BaseEntity {

    @TableId(value = "dataset_id", type = IdType.AUTO)
    private Long datasetId;
    private String datasetCode;
    private String datasetName;
    private Long datasourceId;
    private String tableIds;
    private String semanticModel;
    private String defaultFilters;
    private CommonStatus status;
    private String remark;
}
