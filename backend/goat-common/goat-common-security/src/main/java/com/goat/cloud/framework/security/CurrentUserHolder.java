package com.goat.cloud.framework.security;

import com.goat.cloud.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author wangjubin
 */
public final class CurrentUserHolder {

    private CurrentUserHolder() {
    }

    public static LoginSession require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginSession session)) {
            throw new BusinessException(4010, "Login required");
        }
        return session;
    }

    public static Long getUserIdOrDefault(Long fallback) {
        try {
            return require().getUserId();
        } catch (BusinessException ex) {
            return fallback;
        }
    }
}
