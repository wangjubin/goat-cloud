package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiReportTemplate;
import com.goat.cloud.module.ai.mapper.AiReportTemplateMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import com.goat.cloud.module.ai.runtime.AiRuntimeService;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 报告生成节点执行器
 * <p>
 * 根据 SQL 查询结果生成 ECharts 图表配置
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerationExecutor implements NodeExecutor {

    private final AiRuntimeService runtimeService;
    private final AiReportTemplateMapper reportTemplateMapper;
    private final ObjectMapper objectMapper;

    private static final String CHART_SYSTEM_PROMPT = """
            你是一个数据可视化专家。根据查询结果数据，生成ECharts图表配置。

            规则：
            1. 输出必须是合法的ECharts option JSON
            2. 根据数据特征选择最合适的图表类型（折线图、柱状图、饼图、散点图等）
            3. 添加合适的标题、图例、坐标轴标签
            4. 使用中文标签
            5. 只输出JSON，不要任何解释
            """;

    @Override
    public String getNodeType() {
        return "REPORT_GENERATION";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Object sqlResult = context.get("sqlResult");
            String question = (String) context.getOrDefault("question", "");
            String intent = (String) context.getOrDefault("intent", "DATA_QUERY");

            if (sqlResult == null) {
                return NodeResult.fail("No SQL result available for report generation");
            }

            // 1. 尝试匹配报表模板
            AiReportTemplate template = findMatchingTemplate(intent, question);

            Map<String, Object> result = new LinkedHashMap<>();

            if (template != null) {
                // 使用模板生成图表
                result.put("source", "template");
                result.put("templateCode", template.getTemplateCode());
                result.put("chartType", template.getChartType());
                result = mergeTemplateWithData(result, template, sqlResult);
            } else {
                // 使用 LLM 生成 ECharts 配置
                String chartConfig = generateChartByLLM(question, intent, sqlResult);
                result.put("source", "llm");
                result.put("echartsOption", objectMapper.readValue(chartConfig, Map.class));
            }

            // 添加摘要信息
            result.put("question", question);
            result.put("intent", intent);
            result.put("timestamp", System.currentTimeMillis());

            context.put("reportResult", result);
            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Report generation error", e);
            return NodeResult.fail("Report generation failed: " + e.getMessage());
        }
    }

    private AiReportTemplate findMatchingTemplate(String intent, String question) {
        // 基于意图匹配模板
        String chartType = switch (intent) {
            case "TREND_ANALYSIS" -> "line";
            case "DATA_COMPARE" -> "bar";
            case "REPORT_GENERATION" -> "dashboard";
            default -> null;
        };

        if (chartType != null) {
            AiReportTemplate template = reportTemplateMapper.selectOne(
                    new LambdaQueryWrapper<AiReportTemplate>()
                            .eq(AiReportTemplate::getChartType, chartType)
                            .eq(AiReportTemplate::getStatus, "ENABLED")
                            .last("limit 1")
            );
            if (template != null) return template;
        }

        // 回退：返回任意启用的模板
        return reportTemplateMapper.selectOne(
                new LambdaQueryWrapper<AiReportTemplate>()
                        .eq(AiReportTemplate::getStatus, "ENABLED")
                        .last("limit 1")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeTemplateWithData(Map<String, Object> result, AiReportTemplate template, Object sqlResult) {
        try {
            if (template.getTemplateJson() != null) {
                Map<String, Object> option = objectMapper.readValue(template.getTemplateJson(), Map.class);
                result.put("echartsOption", option);
            }
            if (template.getDataMappingJson() != null) {
                Map<String, Object> mapping = objectMapper.readValue(template.getDataMappingJson(), Map.class);
                result.put("dataMapping", mapping);
            }
        } catch (Exception e) {
            log.warn("Template merge failed, falling back to LLM", e);
        }
        return result;
    }

    private String generateChartByLLM(String question, String intent, Object sqlResult) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("问题：").append(question).append("\n\n");
        userPrompt.append("查询结果数据：\n");

        try {
            String dataJson = objectMapper.writeValueAsString(sqlResult);
            // 限制数据量避免 prompt 过长
            if (dataJson.length() > 4000) {
                dataJson = dataJson.substring(0, 4000) + "...(truncated)";
            }
            userPrompt.append(dataJson);
        } catch (Exception e) {
            userPrompt.append(sqlResult);
        }

        userPrompt.append("\n\n意图：").append(intent);
        userPrompt.append("\n请生成ECharts图表配置JSON。");

        AiChatRequest chatRequest = new AiChatRequest();
        chatRequest.setSystemPrompt(CHART_SYSTEM_PROMPT);
        chatRequest.setMessage(userPrompt.toString());
        chatRequest.setOptions(Map.of("bizType", "CHART_GENERATION"));

        AiChatResponse chatResponse = runtimeService.chat(chatRequest);
        String response = chatResponse.getMessage() != null ? chatResponse.getMessage().getContent() : "{}";
        return cleanJsonOutput(response);
    }

    private String cleanJsonOutput(String output) {
        if (output == null) return "{}";
        String json = output.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        return json.trim();
    }
}