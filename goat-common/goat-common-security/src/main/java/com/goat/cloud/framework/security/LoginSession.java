package com.goat.cloud.framework.security;

import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.DataScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangjubin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginSession implements Serializable {

    private String sessionId;
    private Long userId;
    private String username;
    private String nickname;
    private Long deptId;
    private Boolean superAdmin;
    private CommonStatus status;
    private List<Long> roleIds;
    private List<String> roleCodes;
    private List<String> permissions;
    private DataScope dataScope;
    private List<Long> customDeptIds;
    private LocalDateTime loginTime;
    private LocalDateTime lastAccessTime;
    private LocalDateTime refreshExpireAt;

    public boolean isSuperAdmin() {
        return Boolean.TRUE.equals(superAdmin);
    }

    public List<Long> getRoleIds() {
        return roleIds == null ? new ArrayList<>() : roleIds;
    }

    public List<String> getRoleCodes() {
        return roleCodes == null ? new ArrayList<>() : roleCodes;
    }

    public List<String> getPermissions() {
        return permissions == null ? new ArrayList<>() : permissions;
    }

    public List<Long> getCustomDeptIds() {
        return customDeptIds == null ? new ArrayList<>() : customDeptIds;
    }
}
