package com.goat.cloud.module.system.model.query;

import com.goat.cloud.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RolePageQuery extends PageQuery {

    private String roleCode;
    private String roleName;
    private String status;
}
