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
@TableName("ai_chatbi_table")
@EqualsAndHashCode(callSuper = true)
public class AiChatBiTable extends BaseEntity {

    @TableId(value = "table_id", type = IdType.AUTO)
    private Long tableId;
    private Long datasourceId;
    private String schemaName;
    private String tableName;
    private String tableComment;
    private String columnsJson;
    private CommonStatus status;
    private String remark;
}
