package com.goat.cloud.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goat.cloud.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author wangjubin
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<SysMenu> selectEnabledMenus();

    List<SysMenu> selectManageMenus();

    List<SysMenu> selectMenusByRoleIds(@Param("roleIds") List<Long> roleIds);

    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    List<String> selectPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
