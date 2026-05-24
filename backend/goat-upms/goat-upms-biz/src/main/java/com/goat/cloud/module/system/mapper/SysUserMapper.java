package com.goat.cloud.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.goat.cloud.module.system.entity.SysUser;
import com.goat.cloud.module.system.model.query.UserPageQuery;
import com.goat.cloud.module.system.model.vo.UserDetailVO;
import com.goat.cloud.module.system.model.vo.UserPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author wangjubin
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    IPage<UserPageVO> selectUserPage(IPage<UserPageVO> page, @Param("query") UserPageQuery query);

    UserDetailVO selectUserDetail(@Param("userId") Long userId);

    SysUser selectByUsername(@Param("username") String username);
}
