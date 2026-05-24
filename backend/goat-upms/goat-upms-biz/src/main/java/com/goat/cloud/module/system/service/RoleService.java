package com.goat.cloud.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.DataScope;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.module.system.entity.SysRole;
import com.goat.cloud.module.system.entity.SysRoleDept;
import com.goat.cloud.module.system.entity.SysRoleMenu;
import com.goat.cloud.module.system.mapper.SysRoleDeptMapper;
import com.goat.cloud.module.system.mapper.SysRoleMapper;
import com.goat.cloud.module.system.mapper.SysRoleMenuMapper;
import com.goat.cloud.module.system.model.query.RolePageQuery;
import com.goat.cloud.module.system.model.request.AssignRolePermissionsRequest;
import com.goat.cloud.module.system.model.request.RoleSaveRequest;
import com.goat.cloud.module.system.model.vo.RolePageVO;
import com.goat.cloud.module.system.model.vo.RolePermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleDeptMapper sysRoleDeptMapper;

    public PageResponse<RolePageVO> page(RolePageQuery query) {
        Page<RolePageVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResponse.of(sysRoleMapper.selectRolePage(page, query));
    }

    public SysRole detail(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(4041, "Role not found");
        }
        return role;
    }

    public void save(RoleSaveRequest request) {
        SysRole duplicate = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, request.getRoleCode())
                .ne(request.getRoleId() != null, SysRole::getRoleId, request.getRoleId())
                .last("limit 1"));
        if (duplicate != null) {
            throw new BusinessException(4010, "Role code already exists");
        }
        if (request.getRoleId() == null) {
            SysRole role = new SysRole();
            role.setRoleCode(request.getRoleCode());
            role.setRoleName(request.getRoleName());
            role.setStatus(request.getStatus());
            role.setDataScope(DataScope.SELF);
            role.setRemark(request.getRemark());
            sysRoleMapper.insert(role);
            return;
        }
        SysRole role = detail(request.getRoleId());
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus());
        role.setRemark(request.getRemark());
        sysRoleMapper.updateById(role);
    }

    public void delete(List<Long> ids) {
        sysRoleMapper.deleteByIds(ids);
    }

    public void changeStatus(Long roleId, CommonStatus status) {
        sysRoleMapper.update(new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getRoleId, roleId)
                .set(SysRole::getStatus, status));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(AssignRolePermissionsRequest request) {
        SysRole role = detail(request.getRoleId());
        role.setDataScope(request.getDataScope());
        sysRoleMapper.updateById(role);

        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, request.getRoleId()));
        if (request.getMenuIds() != null) {
            request.getMenuIds().forEach(menuId -> {
                SysRoleMenu item = new SysRoleMenu();
                item.setRoleId(request.getRoleId());
                item.setMenuId(menuId);
                sysRoleMenuMapper.insert(item);
            });
        }

        sysRoleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, request.getRoleId()));
        if (request.getDataScope() == DataScope.CUSTOM && request.getDeptIds() != null) {
            request.getDeptIds().forEach(deptId -> {
                SysRoleDept item = new SysRoleDept();
                item.setRoleId(request.getRoleId());
                item.setDeptId(deptId);
                sysRoleDeptMapper.insert(item);
            });
        }
    }

    public RolePermissionVO getPermissions(Long roleId) {
        SysRole role = detail(roleId);
        RolePermissionVO result = new RolePermissionVO();
        result.setRoleId(roleId);
        result.setDataScope(role.getDataScope());
        result.setMenuIds(sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList());
        result.setDeptIds(sysRoleDeptMapper.selectDeptIdsByRoleId(roleId));
        return result;
    }

    public List<SysRole> listByUserId(Long userId) {
        return sysRoleMapper.selectByUserId(userId);
    }

    public List<Long> listCustomDeptIds(List<Long> roleIds, DataScope scope) {
        if (scope != DataScope.CUSTOM || roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        roleIds.forEach(roleId -> result.addAll(sysRoleDeptMapper.selectDeptIdsByRoleId(roleId)));
        return result.stream().distinct().toList();
    }
}
