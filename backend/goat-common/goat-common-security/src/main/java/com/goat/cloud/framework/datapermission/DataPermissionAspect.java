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
 *
 * TODO: Data permission filtering is not yet implemented.
 * This aspect sets the DataPermissionRuleContext, but no MyBatis interceptor reads it.
 * To complete implementation:
 * 1. Create DataPermissionInterceptor implementing org.apache.ibatis.plugin.Interceptor
 * 2. In the interceptor, read DataPermissionContextHolder.get() to get the rule
 * 3. Based on rule.getDataScope(), inject SQL WHERE clauses:
 *    - ALL: no filter
 *    - DEPT: WHERE dept_id = #{deptId}
 *    - DEPT_AND_CHILDREN: WHERE dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = #{deptId} OR FIND_IN_SET(#{deptId}, ancestors))
 *    - SELF: WHERE create_by = #{userId}
 *    - CUSTOM: WHERE dept_id IN (#{customDeptIds})
 * 4. Register the interceptor in MybatisPlusConfig
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
