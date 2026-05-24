package com.goat.cloud.module.system.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
@Builder
public class AuthUserVO {

    private Long userId;
    private String username;
    private String nickname;
    private Long deptId;
}
