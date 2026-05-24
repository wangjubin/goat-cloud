package com.goat.cloud.module.system.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.module.system.entity.SysRole;
import com.goat.cloud.module.system.model.query.RolePageQuery;
import com.goat.cloud.module.system.model.request.AssignRolePermissionsRequest;
import com.goat.cloud.module.system.model.request.IdsRequest;
import com.goat.cloud.module.system.model.request.RoleSaveRequest;
import com.goat.cloud.module.system.model.request.StatusChangeRequest;
import com.goat.cloud.module.system.model.vo.RolePageVO;
import com.goat.cloud.module.system.model.vo.RolePermissionVO;
import com.goat.cloud.module.system.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/page")
    public ApiResponse<PageResponse<RolePageVO>> page(@RequestBody RolePageQuery query) {
        return ApiResponse.success(roleService.page(query));
    }

    @GetMapping("/{roleId}")
    public ApiResponse<SysRole> detail(@PathVariable Long roleId) {
        return ApiResponse.success(roleService.detail(roleId));
    }

    @PostMapping("/save")
    @PreAuthorize("@pms.has('system:role:save')")
    public ApiResponse<Void> save(@RequestBody @Valid RoleSaveRequest request) {
        roleService.save(request);
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    @PreAuthorize("@pms.has('system:role:save')")
    public ApiResponse<Void> delete(@RequestBody @Valid IdsRequest request) {
        roleService.delete(request.getIds());
        return ApiResponse.success();
    }

    @PostMapping("/status")
    @PreAuthorize("@pms.has('system:role:save')")
    public ApiResponse<Void> status(@RequestBody @Valid StatusChangeRequest request) {
        roleService.changeStatus(request.getId(), request.getStatus());
        return ApiResponse.success();
    }

    @PostMapping("/assign-permissions")
    @PreAuthorize("@pms.has('system:role:save')")
    public ApiResponse<Void> assignPermissions(@RequestBody @Valid AssignRolePermissionsRequest request) {
        roleService.assignPermissions(request);
        return ApiResponse.success();
    }

    @GetMapping("/{roleId}/permissions")
    public ApiResponse<RolePermissionVO> permissions(@PathVariable Long roleId) {
        return ApiResponse.success(roleService.getPermissions(roleId));
    }
}
