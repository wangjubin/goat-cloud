package com.goat.cloud.module.system.model.vo;

import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.MenuType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class MenuTreeVO {

    private Long menuId;
    private Long parentId;
    private String menuName;
    private MenuType menuType;
    private String routePath;
    private String componentPath;
    private String permissionCode;
    private String icon;
    private Integer sortNo;
    private Boolean visible;
    private Boolean keepAlive;
    private Boolean externalLink;
    private CommonStatus status;
    private String remark;
    private List<MenuTreeVO> children = new ArrayList<>();
}
