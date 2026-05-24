package com.goat.cloud.module.system.model.vo;

import com.goat.cloud.common.enums.CommonStatus;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class DeptPageVO {

    private Long deptId;
    private Long parentId;
    private String ancestors;
    private String deptCode;
    private String deptName;
    private String leader;
    private String phone;
    private Integer sortNo;
    private CommonStatus status;
    private String remark;
}
