package com.goat.cloud.module.system.model.request;

import com.goat.cloud.common.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class DeptSaveRequest {

    private Long deptId;

    @NotNull
    private Long parentId;

    @NotBlank
    private String deptCode;

    @NotBlank
    private String deptName;

    private String leader;
    private String phone;
    private Integer sortNo = 0;
    private CommonStatus status = CommonStatus.ENABLED;
    private String remark;
}
