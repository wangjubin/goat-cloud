package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiIntentConfig;
import com.goat.cloud.module.ai.mapper.AiIntentConfigMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/intent-configs")
public class AiIntentConfigController extends BaseAiCrudController<AiIntentConfig> {

    public AiIntentConfigController(AiService aiService, AiIntentConfigMapper mapper) {
        super(aiService, mapper);
    }
}
