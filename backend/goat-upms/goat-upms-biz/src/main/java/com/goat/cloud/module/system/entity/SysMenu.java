package com.goat.cloud.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.MenuType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@TableName("sys_menu")
@EqualsAndHashCode(callSuper = true)
public class SysMenu extends BaseEntity {

    @TableId(type = IdType.AUTO)
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
}
