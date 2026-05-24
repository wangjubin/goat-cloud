package com.goat.cloud.module.ai.controller;

import com.goat.cloud.module.ai.entity.AiBillingRecord;
import com.goat.cloud.module.ai.mapper.AiBillingRecordMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/billing")
public class AiBillingRecordController extends BaseAiCrudController<AiBillingRecord> {

    public AiBillingRecordController(AiService aiService, AiBillingRecordMapper mapper) {
        super(aiService, mapper);
    }
}
