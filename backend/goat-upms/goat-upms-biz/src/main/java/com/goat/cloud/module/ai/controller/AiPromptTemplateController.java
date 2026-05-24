package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiPromptTemplate;
import com.goat.cloud.module.ai.mapper.AiPromptTemplateMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/prompts")
public class AiPromptTemplateController extends BaseAiCrudController<AiPromptTemplate> {

    public AiPromptTemplateController(AiService aiService, AiPromptTemplateMapper mapper) {
        super(aiService, mapper);
    }
}
