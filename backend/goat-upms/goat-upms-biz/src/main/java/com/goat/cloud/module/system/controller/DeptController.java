package com.goat.cloud.module.system.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.module.system.entity.SysDept;
import com.goat.cloud.module.system.model.query.DeptPageQuery;
import com.goat.cloud.module.system.model.request.DeptSaveRequest;
import com.goat.cloud.module.system.model.request.IdsRequest;
import com.goat.cloud.module.system.model.vo.DeptPageVO;
import com.goat.cloud.module.system.model.vo.DeptTreeVO;
import com.goat.cloud.module.system.service.DeptService;
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
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @PostMapping("/page")
    public ApiResponse<PageResponse<DeptPageVO>> page(@RequestBody DeptPageQuery query) {
        return ApiResponse.success(deptService.page(query));
    }

    @PostMapping("/tree")
    public ApiResponse<List<DeptTreeVO>> tree(@RequestBody(required = false) DeptPageQuery query) {
        return ApiResponse.success(deptService.tree(query == null ? new DeptPageQuery() : query));
    }

    @GetMapping("/tree")
    public ApiResponse<List<DeptTreeVO>> tree() {
        return ApiResponse.success(deptService.tree(new DeptPageQuery()));
    }

    @GetMapping("/{deptId}")
    public ApiResponse<SysDept> detail(@PathVariable Long deptId) {
        return ApiResponse.success(deptService.detail(deptId));
    }

    @PostMapping("/save")
    @PreAuthorize("@pms.has('system:dept:save')")
    public ApiResponse<Void> save(@RequestBody @Valid DeptSaveRequest request) {
        deptService.save(request);
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    @PreAuthorize("@pms.has('system:dept:save')")
    public ApiResponse<Void> delete(@RequestBody @Valid IdsRequest request) {
        deptService.delete(request.getIds());
        return ApiResponse.success();
    }
}
