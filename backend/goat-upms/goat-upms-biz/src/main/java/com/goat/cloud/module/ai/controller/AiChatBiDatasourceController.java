package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiChatBiDatasource;
import com.goat.cloud.module.ai.entity.AiChatBiTable;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.service.AiDatasourceImportService;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/datasources")
public class AiChatBiDatasourceController extends BaseAiCrudController<AiChatBiDatasource> {

    private final AiDatasourceImportService importService;

    public AiChatBiDatasourceController(AiService aiService, AiChatBiDatasourceMapper mapper,
                                        AiDatasourceImportService importService) {
        super(aiService, mapper);
        this.importService = importService;
    }

    @PostMapping("/{datasourceId}/test-connection")
    public ApiResponse<Map<String, Object>> testConnection(@PathVariable Long datasourceId) {
        return ApiResponse.success(importService.testConnection(datasourceId));
    }

    @GetMapping("/{datasourceId}/schemas")
    public ApiResponse<List<String>> listSchemas(@PathVariable Long datasourceId) {
        return ApiResponse.success(importService.listSchemas(datasourceId));
    }

    @PostMapping("/{datasourceId}/import-tables")
    public ApiResponse<List<AiChatBiTable>> importTables(
            @PathVariable Long datasourceId,
            @RequestParam(required = false, defaultValue = "public") String schema) {
        return ApiResponse.success(importService.importTables(datasourceId, schema));
    }
}
