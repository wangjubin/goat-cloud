package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_mcp_server")
@EqualsAndHashCode(callSuper = true)
public class AiMcpServer extends BaseEntity {

    @TableId(value = "server_id", type = IdType.AUTO)
    private Long serverId;
    private String serverCode;
    private String serverName;
    private String transportType;
    private String endpoint;
    private String authConfigJson;
    private String capabilitiesJson;
    private String healthStatus;
    private LocalDateTime lastHealthCheck;
    private String status;
}
