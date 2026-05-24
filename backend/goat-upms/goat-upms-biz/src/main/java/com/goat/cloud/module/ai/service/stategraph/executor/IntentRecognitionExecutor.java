package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiIntentConfig;
import com.goat.cloud.module.ai.mapper.AiIntentConfigMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 意图识别节点执行器
 * <p>
 * 解析用户问题，识别意图类型（如查询数据、生成报表、数据对比等）
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRecognitionExecutor implements NodeExecutor {

    private final AiIntentConfigMapper intentConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "INTENT_RECOGNITION";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            String question = (String) context.getOrDefault("question", "");
            if (question == null || question.isBlank()) {
                return NodeResult.fail("Question is empty");
            }

            // 加载意图配置
            List<AiIntentConfig> intents = intentConfigMapper.selectList(
                    new LambdaQueryWrapper<AiIntentConfig>()
                            .eq(AiIntentConfig::getStatus, "ENABLED")
            );

            // 简单关键词匹配意图识别
            String detectedIntent = "DATA_QUERY"; // 默认意图
            double bestScore = 0.0;
            Map<String, Object> intentResult = new LinkedHashMap<>();
            intentResult.put("question", question);

            for (AiIntentConfig intent : intents) {
                double score = calculateIntentScore(question, intent);
                if (score > bestScore) {
                    bestScore = score;
                    detectedIntent = intent.getIntentCode();
                }
            }

            // 基于规则的关键词意图识别
            String lowerQuestion = question.toLowerCase();
            if (lowerQuestion.contains("对比") || lowerQuestion.contains("比较") || lowerQuestion.contains("compare")) {
                detectedIntent = "DATA_COMPARE";
            } else if (lowerQuestion.contains("趋势") || lowerQuestion.contains("trend") || lowerQuestion.contains("变化")) {
                detectedIntent = "TREND_ANALYSIS";
            } else if (lowerQuestion.contains("报表") || lowerQuestion.contains("报告") || lowerQuestion.contains("report")) {
                detectedIntent = "REPORT_GENERATION";
            } else if (lowerQuestion.contains("为什么") || lowerQuestion.contains("原因") || lowerQuestion.contains("why")) {
                detectedIntent = "ROOT_CAUSE";
            }

            intentResult.put("intent", detectedIntent);
            intentResult.put("confidence", bestScore > 0 ? bestScore : 0.6);
            intentResult.put("method", bestScore > 0 ? "config-matching" : "keyword-rules");

            context.put("intent", detectedIntent);
            context.put("intentResult", intentResult);

            return NodeResult.ok(objectMapper.writeValueAsString(intentResult));
        } catch (Exception e) {
            log.error("Intent recognition error", e);
            return NodeResult.fail("Intent recognition failed: " + e.getMessage());
        }
    }

    private double calculateIntentScore(String question, AiIntentConfig intent) {
        try {
            String examplesJson = intent.getExamplesJson();
            if (examplesJson == null || examplesJson.isBlank()) return 0;
            List<String> examples = objectMapper.readValue(examplesJson, new TypeReference<>() {});
            double maxScore = 0;
            String lowerQ = question.toLowerCase();
            for (String example : examples) {
                if (lowerQ.contains(example.toLowerCase())) {
                    maxScore = Math.max(maxScore, 0.8);
                }
            }
            return maxScore;
        } catch (Exception e) {
            return 0;
        }
    }
}