package com.goat.cloud.module.system.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.module.system.model.query.UserPageQuery;
import com.goat.cloud.module.system.model.request.AssignUserRolesRequest;
import com.goat.cloud.module.system.model.request.IdsRequest;
import com.goat.cloud.module.system.model.request.ResetPasswordRequest;
import com.goat.cloud.module.system.model.request.StatusChangeRequest;
import com.goat.cloud.module.system.model.request.UserCreateRequest;
import com.goat.cloud.module.system.model.request.UserUpdateRequest;
import com.goat.cloud.module.system.model.vo.UserDetailVO;
import com.goat.cloud.module.system.model.vo.UserPageVO;
import com.goat.cloud.module.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/page")
    public ApiResponse<PageResponse<UserPageVO>> page(@RequestBody UserPageQuery query) {
        return ApiResponse.success(userService.page(query));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserDetailVO> detail(@PathVariable Long userId) {
        return ApiResponse.success(userService.detail(userId));
    }

    @PostMapping("/create")
    public ApiResponse<Void> create(@RequestBody @Valid UserCreateRequest request) {
        userService.create(request);
        return ApiResponse.success();
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(@RequestBody @Valid UserUpdateRequest request) {
        userService.update(request);
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestBody @Valid IdsRequest request) {
        userService.delete(request.getIds());
        return ApiResponse.success();
    }

    @PostMapping("/status")
    public ApiResponse<Void> status(@RequestBody @Valid StatusChangeRequest request) {
        userService.changeStatus(request.getId(), request.getStatus());
        return ApiResponse.success();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request.getUserId());
        return ApiResponse.success();
    }

    @PostMapping("/assign-roles")
    public ApiResponse<Void> assignRoles(@RequestBody @Valid AssignUserRolesRequest request) {
        userService.assignRoles(request.getUserId(), request.getRoleIds());
        return ApiResponse.success();
    }
}
