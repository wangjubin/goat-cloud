package com.goat.cloud.module.system.model.request;

import com.goat.cloud.common.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class RoleSaveRequest {

    private Long roleId;

    @NotBlank
    private String roleCode;

    @NotBlank
    private String roleName;

    private CommonStatus status = CommonStatus.ENABLED;
    private String remark;
}
