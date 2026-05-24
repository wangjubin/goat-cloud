package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiAgent;
import com.goat.cloud.module.ai.mapper.AiAgentMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/agents")
public class AiAgentController extends BaseAiCrudController<AiAgent> {

    public AiAgentController(AiService aiService, AiAgentMapper mapper) {
        super(aiService, mapper);
    }
}
