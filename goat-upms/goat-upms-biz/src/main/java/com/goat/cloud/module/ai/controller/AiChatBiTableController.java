package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiChatBiTable;
import com.goat.cloud.module.ai.mapper.AiChatBiTableMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/tables")
public class AiChatBiTableController extends BaseAiCrudController<AiChatBiTable> {

    public AiChatBiTableController(AiService aiService, AiChatBiTableMapper mapper) {
        super(aiService, mapper);
    }
}
