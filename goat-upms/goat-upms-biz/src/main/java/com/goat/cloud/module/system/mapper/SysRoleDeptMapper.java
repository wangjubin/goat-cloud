package com.goat.cloud.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goat.cloud.module.system.entity.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author wangjubin
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);
}
