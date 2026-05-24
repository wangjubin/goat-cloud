package com.goat.cloud.framework.security;

/**
 * 权限校验服务，用于 @PreAuthorize("@pms.has('xxx')") 表达式
 * @author wangjubin
 */
import org.springframework.stereotype.Component;

@Component("pms")
public class PermissionService {

    public boolean has(String permission) {
        LoginSession session = CurrentUserHolder.require();
        return session.getPermissions().contains(permission);
    }
}