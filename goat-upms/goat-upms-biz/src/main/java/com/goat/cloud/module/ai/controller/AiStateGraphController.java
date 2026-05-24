package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiStateGraph;
import com.goat.cloud.module.ai.mapper.AiStateGraphMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/stategraphs")
public class AiStateGraphController extends BaseAiCrudController<AiStateGraph> {

    public AiStateGraphController(AiService aiService, AiStateGraphMapper mapper) {
        super(aiService, mapper);
    }
}
