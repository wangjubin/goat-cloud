package com.goat.cloud.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.entity.AiConversationRecord;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.mapper.AiConversationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 模型测试与统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelTestService {

    private final AiModelConfigMapper modelConfigMapper;
    private final AiConversationRecordMapper recordMapper;

    /**
     * 测试模型连通性
     */
    public Map<String, Object> testModelConnectivity(Long modelId) {
        Map<String, Object> result = new HashMap<>();
        try {
            AiModelConfig config = modelConfigMapper.selectById(modelId);
            if (config == null) {
                result.put("success", false);
                result.put("message", "模型配置不存在");
                return result;
            }

            // 检查必要字段
            if (config.getEndpoint() == null || config.getEndpoint().trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "模型端点未配置");
                return result;
            }

            if (config.getApiKeyRef() == null || config.getApiKeyRef().trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "API密钥未配置");
                return result;
            }

            // TODO: 实际发送测试请求到模型端点
            // 这里简化为检查配置完整性
            result.put("success", true);
            result.put("message", "模型配置检查通过");
            result.put("modelId", modelId);
            result.put("modelName", config.getModelName());
            result.put("endpoint", config.getEndpoint());
            result.put("checkedAt", LocalDateTime.now().toString());

            log.info("Model connectivity test passed: modelId={}", modelId);
        } catch (Exception e) {
            log.error("Model connectivity test failed: modelId={}", modelId, e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取模型使用统计
     */
    public Map<String, Object> getModelUsageStats(Long modelId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            AiModelConfig config = modelConfigMapper.selectById(modelId);
            if (config == null) {
                stats.put("error", "模型配置不存在");
                return stats;
            }

            // 统计该模型相关的对话记录数
            LambdaQueryWrapper<AiConversationRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiConversationRecord::getModelId, modelId);
            Long totalCalls = recordMapper.selectCount(wrapper);

            // 统计今日调用
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiConversationRecord::getModelId, modelId)
                   .ge(AiConversationRecord::getCreateTime, todayStart);
            Long todayCalls = recordMapper.selectCount(wrapper);

            // 统计总Token消耗
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiConversationRecord::getModelId, modelId);
            wrapper.select(AiConversationRecord::getPromptTokens, AiConversationRecord::getCompletionTokens);
            var records = recordMapper.selectList(wrapper);
            long totalPromptTokens = records.stream()
                    .mapToLong(r -> r.getPromptTokens() != null ? r.getPromptTokens() : 0)
                    .sum();
            long totalCompletionTokens = records.stream()
                    .mapToLong(r -> r.getCompletionTokens() != null ? r.getCompletionTokens() : 0)
                    .sum();

            stats.put("modelId", modelId);
            stats.put("modelName", config.getModelName());
            stats.put("totalCalls", totalCalls);
            stats.put("todayCalls", todayCalls);
            stats.put("totalPromptTokens", totalPromptTokens);
            stats.put("totalCompletionTokens", totalCompletionTokens);
            stats.put("totalTokens", totalPromptTokens + totalCompletionTokens);

            log.debug("Model usage stats retrieved: modelId={}, totalCalls={}", modelId, totalCalls);
        } catch (Exception e) {
            log.error("Failed to get model usage stats: modelId={}", modelId, e);
            stats.put("error", "获取统计失败: " + e.getMessage());
        }
        return stats;
    }
}
