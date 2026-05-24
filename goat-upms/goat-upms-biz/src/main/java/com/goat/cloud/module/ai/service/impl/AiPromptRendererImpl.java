package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiPromptTemplate;
import com.goat.cloud.module.ai.service.AiPromptRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词渲染服务实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiPromptRendererImpl implements AiPromptRenderer {

    @Override
    public RenderedPrompt render(AiPromptTemplate template, Map<String, Object> variables) {
        String systemPrompt = renderSystemPrompt(template, variables);
        String userPrompt = renderUserPrompt(template, variables);
        List<String> missingVariables = findMissingVariables(template, variables);

        return new RenderedPrompt(systemPrompt, userPrompt, missingVariables);
    }

    @Override
    public String renderSystemPrompt(AiPromptTemplate template, Map<String, Object> variables) {
        if (template.getSystemPrompt() == null) {
            return "";
        }
        return replaceVariables(template.getSystemPrompt(), variables);
    }

    @Override
    public String renderUserPrompt(AiPromptTemplate template, Map<String, Object> variables) {
        if (template.getUserPrompt() == null) {
            return "";
        }
        return replaceVariables(template.getUserPrompt(), variables);
    }

    @Override
    public boolean validateVariables(AiPromptTemplate template, Map<String, Object> variables) {
        List<String> missing = findMissingVariables(template, variables);
        return missing.isEmpty();
    }

    @Override
    public List<String> extractVariables(String template) {
        List<String> variables = new ArrayList<>();
        if (template == null) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        Set<String> found = new HashSet<>();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            if (!found.contains(varName)) {
                found.add(varName);
                variables.add(varName);
            }
        }

        return variables;
    }

    private String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template != null ? template : "";
        }

        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : matcher.group(0);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private List<String> findMissingVariables(AiPromptTemplate template, Map<String, Object> variables) {
        List<String> missing = new ArrayList<>();
        Set<String> providedVars = variables != null ? variables.keySet() : Set.of();

        List<String> systemVars = extractVariables(template.getSystemPrompt());
        for (String var : systemVars) {
            if (!providedVars.contains(var)) {
                missing.add(var);
            }
        }

        List<String> userVars = extractVariables(template.getUserPrompt());
        for (String var : userVars) {
            if (!providedVars.contains(var) && !missing.contains(var)) {
                missing.add(var);
            }
        }

        return missing;
    }
}
