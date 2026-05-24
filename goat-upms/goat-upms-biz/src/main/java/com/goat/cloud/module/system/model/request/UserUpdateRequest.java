package com.goat.cloud.module.system.model.request;

import com.goat.cloud.common.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class UserUpdateRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String nickname;

    @NotNull
    private Long deptId;

    private String phone;
    private String email;
    private CommonStatus status;
    private String remark;
}
