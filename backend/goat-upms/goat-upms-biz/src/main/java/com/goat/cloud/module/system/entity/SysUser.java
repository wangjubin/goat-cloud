package com.goat.cloud.module.system.entity;

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
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long userId;
    private String username;
    private String nickname;
    private String password;
    private Long deptId;
    private String phone;
    private String email;
    private CommonStatus status;
    private Boolean superAdmin;
    private String remark;
}
