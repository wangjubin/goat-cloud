package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiChatBiDatasource;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/datasources")
public class AiChatBiDatasourceController extends BaseAiCrudController<AiChatBiDatasource> {

    public AiChatBiDatasourceController(AiService aiService, AiChatBiDatasourceMapper mapper) {
        super(aiService, mapper);
    }
}
