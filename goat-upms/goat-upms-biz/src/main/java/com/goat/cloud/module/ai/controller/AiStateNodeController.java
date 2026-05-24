package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiStateNode;
import com.goat.cloud.module.ai.mapper.AiStateNodeMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/state-nodes")
public class AiStateNodeController extends BaseAiCrudController<AiStateNode> {

    public AiStateNodeController(AiService aiService, AiStateNodeMapper mapper) {
        super(aiService, mapper);
    }
}
