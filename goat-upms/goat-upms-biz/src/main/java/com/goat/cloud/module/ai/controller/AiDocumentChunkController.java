package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiDocumentChunk;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chunks")
public class AiDocumentChunkController extends BaseAiCrudController<AiDocumentChunk> {

    public AiDocumentChunkController(AiService aiService, AiDocumentChunkMapper mapper) {
        super(aiService, mapper);
    }
}
