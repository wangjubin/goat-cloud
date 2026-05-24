package com.goat.cloud.module.system.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.system.entity.SysMenu;
import com.goat.cloud.module.system.model.request.MenuSaveRequest;
import com.goat.cloud.module.system.model.vo.MenuTreeVO;
import com.goat.cloud.module.system.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeVO>> tree() {
        return ApiResponse.success(menuService.tree());
    }

    @GetMapping("/manage-tree")
    public ApiResponse<List<MenuTreeVO>> manageTree() {
        return ApiResponse.success(menuService.manageTree());
    }

    @GetMapping("/{menuId}")
    public ApiResponse<SysMenu> detail(@PathVariable Long menuId) {
        return ApiResponse.success(menuService.detail(menuId));
    }

    @PostMapping("/save")
    @PreAuthorize("@pms.has('system:menu:save')")
    public ApiResponse<Void> save(@RequestBody @Valid MenuSaveRequest request) {
        menuService.save(request);
        return ApiResponse.success();
    }

    @PostMapping("/delete/{menuId}")
    @PreAuthorize("@pms.has('system:menu:save')")
    public ApiResponse<Void> delete(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return ApiResponse.success();
    }
}
