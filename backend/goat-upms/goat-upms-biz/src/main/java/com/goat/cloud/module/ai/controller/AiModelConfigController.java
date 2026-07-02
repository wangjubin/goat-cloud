package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.service.AiService;
import com.goat.cloud.module.ai.service.AiModelTestService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/models")
public class AiModelConfigController extends BaseAiCrudController<AiModelConfig> {

    private final AiModelTestService modelTestService;

    public AiModelConfigController(AiService aiService, AiModelConfigMapper mapper,
                                   AiModelTestService modelTestService) {
        super(aiService, mapper);
        this.modelTestService = modelTestService;
    }

    /**
     * 测试模型连通性
     */
    @PostMapping("/{id}/test")
    public ApiResponse<Map<String, Object>> testModel(@PathVariable Long id) {
        Map<String, Object> result = modelTestService.testModelConnectivity(id);
        return ApiResponse.success(result);
    }

    /**
     * 获取模型使用统计
     */
    @GetMapping("/{id}/usage")
    public ApiResponse<Map<String, Object>> getModelUsage(@PathVariable Long id) {
        Map<String, Object> usage = modelTestService.getModelUsageStats(id);
        return ApiResponse.success(usage);
    }
}
