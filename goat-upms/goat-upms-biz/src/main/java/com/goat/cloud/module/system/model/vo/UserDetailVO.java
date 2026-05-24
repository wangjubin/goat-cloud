package com.goat.cloud.module.system.model.vo;

import com.goat.cloud.common.enums.CommonStatus;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class UserDetailVO {

    private Long userId;
    private String username;
    private String nickname;
    private Long deptId;
    private String deptName;
    private String phone;
    private String email;
    private CommonStatus status;
    private Boolean superAdmin;
    private String remark;
    private List<Long> roleIds;
}
