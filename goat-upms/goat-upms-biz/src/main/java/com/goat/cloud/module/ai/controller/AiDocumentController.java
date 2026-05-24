package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.mapper.AiDocumentMapper;
import com.goat.cloud.module.ai.model.request.AiIdsRequest;
import com.goat.cloud.module.ai.model.request.AiPageQuery;
import com.goat.cloud.module.ai.service.AiDocumentService;
import com.goat.cloud.module.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/documents")
@RequiredArgsConstructor
public class AiDocumentController {

    private final AiService aiService;
    private final AiDocumentMapper mapper;
    private final AiDocumentService documentService;

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.success(aiService.list(mapper));
    }

    @PostMapping("/page")
    public ApiResponse<?> page(@RequestBody(required = false) AiPageQuery query) {
        return ApiResponse.success(aiService.page(mapper, query));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.success(aiService.detail(mapper, id));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody @Valid AiDocument entity) {
        aiService.save(mapper, entity);
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestBody @Valid AiIdsRequest request) {
        aiService.delete(mapper, request.getIds());
        return ApiResponse.success();
    }

    @PostMapping("/upload")
    public ApiResponse<AiDocumentService.UploadResult> upload(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            AiDocumentService.UploadResult result = documentService.uploadAndParse(
                    knowledgeBaseId, file.getOriginalFilename(), content);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.fail(1, "上传失败: " + e.getMessage());
        }
    }
}
