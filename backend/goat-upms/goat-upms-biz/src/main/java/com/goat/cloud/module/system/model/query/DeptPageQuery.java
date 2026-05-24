package com.goat.cloud.module.system.model.query;

import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeptPageQuery extends PageQuery {

    private String deptName;
    private CommonStatus status;
}
