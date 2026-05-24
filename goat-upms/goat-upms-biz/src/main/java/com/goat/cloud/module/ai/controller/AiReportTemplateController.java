package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiReportTemplate;
import com.goat.cloud.module.ai.mapper.AiReportTemplateMapper;
import com.goat.cloud.module.ai.service.AiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/report-templates")
public class AiReportTemplateController extends BaseAiCrudController<AiReportTemplate> {

    public AiReportTemplateController(AiService aiService, AiReportTemplateMapper mapper) {
        super(aiService, mapper);
    }
}
