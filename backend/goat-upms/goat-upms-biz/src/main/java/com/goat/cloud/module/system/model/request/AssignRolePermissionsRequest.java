package com.goat.cloud.module.system.model.request;

import com.goat.cloud.common.enums.DataScope;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class AssignRolePermissionsRequest {

    @NotNull
    private Long roleId;

    private List<Long> menuIds;

    @NotNull
    private DataScope dataScope;

    private List<Long> deptIds;
}
