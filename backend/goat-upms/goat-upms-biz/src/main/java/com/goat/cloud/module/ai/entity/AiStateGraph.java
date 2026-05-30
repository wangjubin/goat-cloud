package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import com.goat.cloud.framework.config.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@TableName(value = "ai_stategraph", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
public class AiStateGraph extends BaseEntity {

    @TableId(value = "graph_id", type = IdType.AUTO)
    private Long graphId;
    private String graphCode;
    private String graphName;
    private String description;
    private String version;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String definitionJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String configJson;
    private String status;
    private String graphType;
    private Long parentGraphId;
    private Long defaultModelId;
}
