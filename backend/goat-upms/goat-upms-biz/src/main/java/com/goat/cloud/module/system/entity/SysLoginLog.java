package com.goat.cloud.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long logId;
    private String username;
    private Boolean success;
    private String ipAddress;
    private String userAgent;
    private String message;
    private LocalDateTime loginTime;
}
