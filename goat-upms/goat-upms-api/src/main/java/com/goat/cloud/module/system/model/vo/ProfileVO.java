package com.goat.cloud.module.system.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
@Builder
public class ProfileVO {

    private AuthUserVO user;
    private List<String> roleCodes;
    private List<String> permissions;
    private List<RouteVO> routes;
}
