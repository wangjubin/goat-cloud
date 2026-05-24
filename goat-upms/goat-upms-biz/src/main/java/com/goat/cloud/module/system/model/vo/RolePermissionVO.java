package com.goat.cloud.module.system.model.vo;

import com.goat.cloud.common.enums.DataScope;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class RolePermissionVO {

    private Long roleId;
    private DataScope dataScope;
    private List<Long> menuIds;
    private List<Long> deptIds;
}
