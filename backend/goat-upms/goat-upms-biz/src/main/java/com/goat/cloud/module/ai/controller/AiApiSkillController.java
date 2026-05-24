package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiApiSkill;
import com.goat.cloud.module.ai.mapper.AiApiSkillMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/api-skills")
public class AiApiSkillController extends BaseAiCrudController<AiApiSkill> {

    public AiApiSkillController(AiService aiService, AiApiSkillMapper mapper) {
        super(aiService, mapper);
    }
}
