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
@TableName("ai_chatbi_datasource")
@EqualsAndHashCode(callSuper = true)
public class AiChatBiDatasource extends BaseEntity {

    @TableId(value = "datasource_id", type = IdType.AUTO)
    private Long datasourceId;
    private String datasourceCode;
    private String datasourceName;
    private String datasourceType;
    private String jdbcUrl;
    private String username;
    private String credentialRef;
    private CommonStatus status;
    private String remark;
}
