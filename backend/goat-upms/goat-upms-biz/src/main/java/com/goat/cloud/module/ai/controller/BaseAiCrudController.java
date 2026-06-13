package com.goat.cloud.module.ai.controller;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.module.ai.model.request.AiIdsRequest;
import com.goat.cloud.module.ai.model.request.AiPageQuery;
import com.goat.cloud.module.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author wangjubin
 */
@RequiredArgsConstructor
public abstract class BaseAiCrudController<T> {

    private final AiService aiService;
    private final BaseMapper<T> mapper;

    @GetMapping("/list")
    public ApiResponse<List<T>> list() {
        return ApiResponse.success(aiService.list(mapper));
    }

    @PostMapping("/page")
    public ApiResponse<PageResponse<T>> page(@RequestBody(required = false) AiPageQuery query) {
        return ApiResponse.success(aiService.page(mapper, query));
    }

    @GetMapping("/{id}")
    public ApiResponse<T> detail(@PathVariable Long id) {
        return ApiResponse.success(aiService.detail(mapper, id));
    }

    @PostMapping("/save")
    @PreAuthorize("@pms.has('ai:config:save')")
    public ApiResponse<Void> save(@RequestBody @Valid T entity) {
        aiService.save(mapper, entity);
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    @PreAuthorize("@pms.has('ai:config:delete')")
    public ApiResponse<Void> delete(@RequestBody @Valid AiIdsRequest request) {
        aiService.delete(mapper, request.getIds());
        return ApiResponse.success();
    }
}
