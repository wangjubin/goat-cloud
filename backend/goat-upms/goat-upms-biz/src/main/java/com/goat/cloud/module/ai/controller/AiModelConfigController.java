package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/models")
public class AiModelConfigController extends BaseAiCrudController<AiModelConfig> {

    public AiModelConfigController(AiService aiService, AiModelConfigMapper mapper) {
        super(aiService, mapper);
    }
}
