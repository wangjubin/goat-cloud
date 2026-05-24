package com.goat.cloud.framework.datapermission;

import com.goat.cloud.framework.security.CurrentUserHolder;
import com.goat.cloud.framework.security.LoginSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author wangjubin
 */
@Slf4j
@Aspect
@Component
public class DataPermissionAspect {

    @Around("@annotation(dataPermission)")
    public Object around(ProceedingJoinPoint joinPoint, DataPermission dataPermission) throws Throwable {
        LoginSession session = CurrentUserHolder.require();
        DataPermissionRule rule = new DataPermissionRule(
                dataPermission.target(),
                session.isSuperAdmin(),
                session.getUserId(),
                session.getDeptId(),
                session.getDataScope(),
                session.getCustomDeptIds()
        );
        DataPermissionContextHolder.set(rule);
        try {
            return joinPoint.proceed();
        } finally {
            DataPermissionContextHolder.clear();
        }
    }
}
