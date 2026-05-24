package com.goat.cloud.module.system.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class AssignUserRolesRequest {

    @NotNull
    private Long userId;

    private List<Long> roleIds;
}
