package com.goat.cloud.module.system.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class RouteVO {

    private Long menuId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Boolean visible;
    private Boolean keepAlive;
    private Boolean externalLink;
    private List<RouteVO> children = new ArrayList<>();
}
