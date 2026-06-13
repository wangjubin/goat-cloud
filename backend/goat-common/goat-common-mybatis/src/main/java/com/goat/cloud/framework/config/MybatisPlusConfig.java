package com.goat.cloud.framework.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                Long userId = resolveUserId();
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "createBy", Long.class, userId);
                strictInsertFill(metaObject, "updateBy", Long.class, userId);
                strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                Long userId = resolveUserId();
                strictUpdateFill(metaObject, "updateBy", Long.class, userId);
            }

            private Long resolveUserId() {
                try {
                    // Use reflection to avoid cyclic dependency with goat-common-security
                    Class<?> holderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
                    Method getContextMethod = holderClass.getMethod("getContext");
                    Object context = getContextMethod.invoke(null);
                    if (context == null) return 0L;

                    Method getAuthMethod = context.getClass().getMethod("getAuthentication");
                    Object auth = getAuthMethod.invoke(context);
                    if (auth == null) return 0L;

                    Method getPrincipalMethod = auth.getClass().getMethod("getPrincipal");
                    Object principal = getPrincipalMethod.invoke(auth);
                    if (principal == null) return 0L;

                    Method getUserIdMethod = principal.getClass().getMethod("getUserId");
                    Object result = getUserIdMethod.invoke(principal);
                    if (result instanceof Long userId) {
                        return userId;
                    }
                } catch (Exception ignored) {
                }
                return 0L;
            }
        };
    }
}
