package com.goat.cloud.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.goat.cloud.module.system.entity.SysRole;
import com.goat.cloud.module.system.model.query.RolePageQuery;
import com.goat.cloud.module.system.model.vo.RolePageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author wangjubin
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    IPage<RolePageVO> selectRolePage(IPage<RolePageVO> page, @Param("query") RolePageQuery query);

    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
