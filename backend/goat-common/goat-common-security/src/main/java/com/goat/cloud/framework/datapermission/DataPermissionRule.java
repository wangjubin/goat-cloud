package com.goat.cloud.framework.datapermission;

import com.goat.cloud.common.enums.DataScope;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangjubin
 */
@Data
@AllArgsConstructor
public class DataPermissionRule {

    private DataPermissionTarget target;
    private boolean superAdmin;
    private Long userId;
    private Long deptId;
    private DataScope dataScope;
    private List<Long> customDeptIds;

    public List<Long> getCustomDeptIds() {
        return customDeptIds == null ? new ArrayList<>() : customDeptIds;
    }
}
