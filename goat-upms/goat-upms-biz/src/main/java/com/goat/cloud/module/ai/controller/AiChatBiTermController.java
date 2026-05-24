package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiChatBiTerm;
import com.goat.cloud.module.ai.mapper.AiChatBiTermMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/terms")
public class AiChatBiTermController extends BaseAiCrudController<AiChatBiTerm> {

    public AiChatBiTermController(AiService aiService, AiChatBiTermMapper mapper) {
        super(aiService, mapper);
    }
}
