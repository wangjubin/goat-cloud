package com.goat.cloud.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.framework.config.SecurityProperties;
import com.goat.cloud.framework.datapermission.DataPermission;
import com.goat.cloud.framework.datapermission.DataPermissionTarget;
import com.goat.cloud.framework.security.CurrentUserHolder;
import com.goat.cloud.module.system.entity.SysUser;
import com.goat.cloud.module.system.entity.SysUserRole;
import com.goat.cloud.module.system.mapper.SysUserMapper;
import com.goat.cloud.module.system.mapper.SysUserRoleMapper;
import com.goat.cloud.module.system.model.query.UserPageQuery;
import com.goat.cloud.module.system.model.request.UserCreateRequest;
import com.goat.cloud.module.system.model.request.UserUpdateRequest;
import com.goat.cloud.module.system.model.vo.UserDetailVO;
import com.goat.cloud.module.system.model.vo.UserPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    @DataPermission(target = DataPermissionTarget.USER)
    public PageResponse<UserPageVO> page(UserPageQuery query) {
        Page<UserPageVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResponse.of(sysUserMapper.selectUserPage(page, query));
    }

    public UserDetailVO detail(Long userId) {
        UserDetailVO detail = sysUserMapper.selectUserDetail(userId);
        if (detail == null) {
            throw new BusinessException(4040, "User not found");
        }
        detail.setRoleIds(listRoleIdsByUserId(userId));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(UserCreateRequest request) {
        SysUser exists = sysUserMapper.selectByUsername(request.getUsername());
        if (exists != null) {
            throw new BusinessException(4005, "Username already exists");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setDeptId(request.getDeptId());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus());
        user.setSuperAdmin(Boolean.FALSE);
        user.setRemark(request.getRemark());
        // 使用自定义密码或系统默认初始密码
        String rawPassword = (request.getPassword() != null && !request.getPassword().isEmpty())
                ? request.getPassword()
                : securityProperties.getInitialPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        sysUserMapper.insert(user);
    }

    public void update(UserUpdateRequest request) {
        SysUser user = requireUser(request.getUserId());
        user.setNickname(request.getNickname());
        user.setDeptId(request.getDeptId());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus());
        user.setRemark(request.getRemark());
        sysUserMapper.updateById(user);
    }

    public void delete(List<Long> userIds) {
        if (userIds.contains(1L)) {
            throw new BusinessException(4006, "Super administrator cannot be deleted");
        }
        sysUserMapper.deleteByIds(userIds);
    }

    public void changeStatus(Long userId, CommonStatus status) {
        if (userId.equals(1L)) {
            throw new BusinessException(4007, "Super administrator status cannot be changed");
        }
        sysUserMapper.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getStatus, status));
    }

    public void resetPassword(Long userId) {
        if (userId.equals(1L)) {
            throw new BusinessException(4008, "Super administrator password cannot be reset here");
        }
        sysUserMapper.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getPassword, passwordEncoder.encode(securityProperties.getInitialPassword())));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        requireUser(userId);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.forEach(roleId -> {
            SysUserRole item = new SysUserRole();
            item.setUserId(userId);
            item.setRoleId(roleId);
            sysUserRoleMapper.insert(item);
        });
    }

    public SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(4040, "User not found");
        }
        return user;
    }

    public void changeOwnPassword(String oldPassword, String newPassword) {
        SysUser user = requireUser(CurrentUserHolder.require().getUserId());
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(4009, "Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    public List<Long> listRoleIdsByUserId(Long userId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }
}
