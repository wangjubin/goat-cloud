package com.goat.cloud.module.system.model.request;

import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.MenuType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class MenuSaveRequest {

    private Long menuId;

    @NotNull
    private Long parentId;

    @NotBlank
    private String menuName;

    @NotNull
    private MenuType menuType;

    private String routePath;
    private String componentPath;
    private String permissionCode;
    private String icon;
    private Integer sortNo = 0;
    private Boolean visible = Boolean.TRUE;
    private Boolean keepAlive = Boolean.FALSE;
    private Boolean externalLink = Boolean.FALSE;
    private CommonStatus status = CommonStatus.ENABLED;
    private String remark;
}
