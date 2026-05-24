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
@TableName("ai_state_node")
@EqualsAndHashCode(callSuper = true)
public class AiStateNode extends BaseEntity {

    @TableId(value = "node_id", type = IdType.AUTO)
    private Long nodeId;
    private Long graphId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private String configJson;
    private String inputSchema;
    private String outputSchema;
    private String retryConfig;
    private Integer timeoutMs;
    private Integer sortOrder;
    private String edgesJson;
}
