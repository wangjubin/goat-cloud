package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiWorkflow;
import com.goat.cloud.module.ai.mapper.AiWorkflowMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/workflows")
public class AiWorkflowController extends BaseAiCrudController<AiWorkflow> {

    public AiWorkflowController(AiService aiService, AiWorkflowMapper mapper) {
        super(aiService, mapper);
    }
}
