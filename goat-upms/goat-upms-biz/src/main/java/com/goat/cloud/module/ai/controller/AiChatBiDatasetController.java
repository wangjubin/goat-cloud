package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiChatBiDataset;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasetMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/datasets")
public class AiChatBiDatasetController extends BaseAiCrudController<AiChatBiDataset> {

    public AiChatBiDatasetController(AiService aiService, AiChatBiDatasetMapper mapper) {
        super(aiService, mapper);
    }
}
