package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiKnowledgeBase;
import com.goat.cloud.module.ai.mapper.AiKnowledgeBaseMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/knowledge-bases")
public class AiKnowledgeBaseController extends BaseAiCrudController<AiKnowledgeBase> {

    public AiKnowledgeBaseController(AiService aiService, AiKnowledgeBaseMapper mapper) {
        super(aiService, mapper);
    }
}
