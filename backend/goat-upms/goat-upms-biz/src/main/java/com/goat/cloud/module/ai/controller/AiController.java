package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.model.AiCatalogItem;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.model.vo.AiOverviewVO;
import com.goat.cloud.module.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/overview")
    public ApiResponse<AiOverviewVO> overview() {
        return ApiResponse.success(aiService.overview());
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody @Valid AiChatRequest request) {
        return ApiResponse.success(aiService.chat(request));
    }

    @GetMapping("/{section}")
    public ApiResponse<List<AiCatalogItem>> list(@PathVariable String section) {
        return ApiResponse.success(aiService.list(section));
    }
}
