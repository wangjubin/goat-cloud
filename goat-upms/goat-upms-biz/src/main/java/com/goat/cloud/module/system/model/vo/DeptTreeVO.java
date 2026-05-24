package com.goat.cloud.module.system.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class DeptTreeVO {

    private Long deptId;
    private Long parentId;
    private String deptName;
    private String deptCode;
    private List<DeptTreeVO> children = new ArrayList<>();
}
