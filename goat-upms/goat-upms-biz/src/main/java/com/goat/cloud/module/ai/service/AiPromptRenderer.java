package com.goat.cloud.module.ai.service;

import com.goat.cloud.module.ai.entity.AiPromptTemplate;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词渲染服务接口
 * @author wangjubin
 */
public interface AiPromptRenderer {

    Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    /**
     * 渲染提示词模板
     */
    RenderedPrompt render(AiPromptTemplate template, Map<String, Object> variables);

    /**
     * 渲染系统提示词
     */
    String renderSystemPrompt(AiPromptTemplate template, Map<String, Object> variables);

    /**
     * 渲染用户提示词
     */
    String renderUserPrompt(AiPromptTemplate template, Map<String, Object> variables);

    /**
     * 验证变量是否完整
     */
    boolean validateVariables(AiPromptTemplate template, Map<String, Object> variables);

    /**
     * 提取模板中的变量名
     */
    List<String> extractVariables(String template);

    /**
     * 渲染结果
     */
    record RenderedPrompt(
            String systemPrompt,
            String userPrompt,
            List<String> missingVariables
    ) {}
}
