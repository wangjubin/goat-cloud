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
@TableName(value = "ai_state_node", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
public class AiStateNode extends BaseEntity {

    @TableId(value = "node_id", type = IdType.AUTO)
    private Long nodeId;
    private Long graphId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String configJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String inputSchema;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String outputSchema;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String retryConfig;
    private Integer timeoutMs;
    private Integer sortOrder;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String edgesJson;
}
