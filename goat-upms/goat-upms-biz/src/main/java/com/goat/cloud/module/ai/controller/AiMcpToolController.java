package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiMcpTool;
import com.goat.cloud.module.ai.mapper.AiMcpToolMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/mcp-tools")
public class AiMcpToolController extends BaseAiCrudController<AiMcpTool> {

    public AiMcpToolController(AiService aiService, AiMcpToolMapper mapper) {
        super(aiService, mapper);
    }
}
