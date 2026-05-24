package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiVectorConfig;
import com.goat.cloud.module.ai.mapper.AiVectorConfigMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/vector-configs")
public class AiVectorConfigController extends BaseAiCrudController<AiVectorConfig> {

    public AiVectorConfigController(AiService aiService, AiVectorConfigMapper mapper) {
        super(aiService, mapper);
    }
}
