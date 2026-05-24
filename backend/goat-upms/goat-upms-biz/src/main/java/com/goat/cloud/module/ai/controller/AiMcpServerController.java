package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiMcpServer;
import com.goat.cloud.module.ai.mapper.AiMcpServerMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/mcp-servers")
public class AiMcpServerController extends BaseAiCrudController<AiMcpServer> {

    public AiMcpServerController(AiService aiService, AiMcpServerMapper mapper) {
        super(aiService, mapper);
    }
}
