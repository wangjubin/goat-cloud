package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_mcp_tool")
@EqualsAndHashCode(callSuper = true)
public class AiMcpTool extends BaseEntity {

    @TableId(value = "mcp_tool_id", type = IdType.AUTO)
    private Long mcpToolId;
    private String toolCode;
    private String toolName;
    private String serverName;
    private String transportType;
    private String endpoint;
    private String inputSchema;
    private String outputSchema;
    private CommonStatus status;
    private String remark;
}
