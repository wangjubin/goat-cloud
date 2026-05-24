package com.goat.cloud.framework.datapermission;

/**
 * @author wangjubin
 */
public final class DataPermissionContextHolder {

    private static final ThreadLocal<DataPermissionRule> HOLDER = new ThreadLocal<>();

    private DataPermissionContextHolder() {
    }

    public static void set(DataPermissionRule rule) {
        HOLDER.set(rule);
    }

    public static DataPermissionRule get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
