package com.goat.cloud.module.system.model.vo;

import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.DataScope;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class RolePageVO {

    private Long roleId;
    private String roleCode;
    private String roleName;
    private DataScope dataScope;
    private CommonStatus status;
    private String remark;
}
