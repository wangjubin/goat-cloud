package com.goat.cloud.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.module.ai.entity.AiBillingRecord;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiBillingRecordMapper;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * AI计费服务
 * 负责计算模型调用成本、生成账单、预算告警
 * 
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBillingService {

    private final AiBillingRecordMapper billingRecordMapper;
    private final AiModelConfigMapper modelConfigMapper;
    
    // 模型定价配置 (每1000 token的价格，单位：元)
    // 实际项目中应该从数据库或配置中心读取
    private static final Map<String, BigDecimal> MODEL_PRICING = new HashMap<>();
    
    static {
        // GPT系列
        MODEL_PRICING.put("gpt-4", new BigDecimal("0.42"));
        MODEL_PRICING.put("gpt-4-turbo", new BigDecimal("0.14"));
        MODEL_PRICING.put("gpt-3.5-turbo", new BigDecimal("0.014"));
        
        // Claude系列
        MODEL_PRICING.put("claude-3-opus", new BigDecimal("0.105"));
        MODEL_PRICING.put("claude-3-sonnet", new BigDecimal("0.021"));
        MODEL_PRICING.put("claude-3-haiku", new BigDecimal("0.0035"));
        
        // 国产模型
        MODEL_PRICING.put("qwen-max", new BigDecimal("0.14"));
        MODEL_PRICING.put("qwen-plus", new BigDecimal("0.056"));
        MODEL_PRICING.put("ernie-4.0", new BigDecimal("0.168"));
        MODEL_PRICING.put("ernie-3.5", new BigDecimal("0.084"));
        
        // 默认价格
        MODEL_PRICING.put("default", new BigDecimal("0.07"));
    }
    
    // 预算阈值 (单位：元)
    private static final BigDecimal BUDGET_WARNING_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal BUDGET_CRITICAL_THRESHOLD = new BigDecimal("5000");

    /**
     * 记录计费信息
     */
    public void recordBilling(String conversationId, Long modelId, String bizType,
                             Integer promptTokens, Integer completionTokens) {
        try {
            AiModelConfig model = modelConfigMapper.selectById(modelId);
            if (model == null) {
                log.warn("Model not found for billing: modelId={}", modelId);
                return;
            }
            
            // 计算成本
            BigDecimal cost = calculateCost(model.getModelCode(), promptTokens, completionTokens);
            
            AiBillingRecord record = new AiBillingRecord();
            record.setConversationId(conversationId);
            record.setProvider(model.getProvider());
            record.setModelCode(model.getModelCode());
            record.setBizType(bizType);
            record.setPromptTokens(promptTokens);
            record.setCompletionTokens(completionTokens);
            record.setTotalTokens(promptTokens + completionTokens);
            record.setCostAmount(cost);
            record.setCurrency("CNY");
            record.setRequestTime(LocalDateTime.now());
            record.setStatus("SUCCESS");
            
            billingRecordMapper.insert(record);
            
            log.debug("Billing recorded: model={}, tokens={}, cost={}", 
                     model.getModelCode(), promptTokens + completionTokens, cost);
            
            // 检查预算告警
            checkBudgetAlert(model.getProvider());
            
        } catch (Exception e) {
            log.error("Failed to record billing: modelId={}", modelId, e);
        }
    }

    /**
     * 计算调用成本
     */
    public BigDecimal calculateCost(String modelCode, Integer promptTokens, Integer completionTokens) {
        if (promptTokens == null || completionTokens == null) {
            return BigDecimal.ZERO;
        }
        
        // 获取模型定价 (每1000 token)
        BigDecimal pricePerK = MODEL_PRICING.getOrDefault(
                modelCode.toLowerCase(), 
                MODEL_PRICING.get("default")
        );
        
        // 计算总token数 (千)
        int totalTokens = promptTokens + completionTokens;
        BigDecimal tokenK = new BigDecimal(totalTokens).divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        
        // 计算成本
        return pricePerK.multiply(tokenK).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 获取今日成本统计
     */
    public Map<String, Object> getTodayCostStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        return getCostStats(startOfDay, endOfDay);
    }

    /**
     * 获取指定时间范围的成本统计
     */
    public Map<String, Object> getCostStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            LambdaQueryWrapper<AiBillingRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(AiBillingRecord::getRequestTime, startTime)
                   .le(AiBillingRecord::getRequestTime, endTime);
            
            List<AiBillingRecord> records = billingRecordMapper.selectList(wrapper);
            
            // 基础统计
            long totalCalls = records.size();
            long totalPromptTokens = records.stream()
                    .mapToLong(r -> r.getPromptTokens() != null ? r.getPromptTokens() : 0)
                    .sum();
            long totalCompletionTokens = records.stream()
                    .mapToLong(r -> r.getCompletionTokens() != null ? r.getCompletionTokens() : 0)
                    .sum();
            long totalTokens = totalPromptTokens + totalCompletionTokens;
            BigDecimal totalCost = records.stream()
                    .map(r -> r.getCostAmount() != null ? r.getCostAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            stats.put("totalCalls", totalCalls);
            stats.put("totalPromptTokens", totalPromptTokens);
            stats.put("totalCompletionTokens", totalCompletionTokens);
            stats.put("totalTokens", totalTokens);
            stats.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
            stats.put("currency", "CNY");
            
            // 按模型分组统计
            Map<String, Map<String, Object>> modelStats = new HashMap<>();
            for (AiBillingRecord record : records) {
                String modelCode = record.getModelCode();
                modelStats.computeIfAbsent(modelCode, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("modelCode", k);
                    m.put("calls", 0L);
                    m.put("tokens", 0L);
                    m.put("cost", BigDecimal.ZERO);
                    return m;
                });
                
                Map<String, Object> m = modelStats.get(modelCode);
                m.put("calls", (Long) m.get("calls") + 1);
                m.put("tokens", (Long) m.get("tokens") + 
                        (record.getTotalTokens() != null ? record.getTotalTokens() : 0));
                m.put("cost", ((BigDecimal) m.get("cost")).add(
                        record.getCostAmount() != null ? record.getCostAmount() : BigDecimal.ZERO));
            }
            
            stats.put("modelStats", new ArrayList<>(modelStats.values()));
            
            // 按提供商分组统计
            Map<String, BigDecimal> providerCosts = new HashMap<>();
            for (AiBillingRecord record : records) {
                String provider = record.getProvider();
                providerCosts.merge(provider, 
                        record.getCostAmount() != null ? record.getCostAmount() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
            stats.put("providerCosts", providerCosts);
            
            // 预算状态
            stats.put("budgetStatus", getBudgetStatus(totalCost));
            
        } catch (Exception e) {
            log.error("Failed to get cost stats", e);
            stats.put("error", "获取成本统计失败: " + e.getMessage());
        }
        
        return stats;
    }

    /**
     * 检查预算告警
     */
    private void checkBudgetAlert(String provider) {
        try {
            // 获取本月成本
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime now = LocalDateTime.now();
            
            LambdaQueryWrapper<AiBillingRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(AiBillingRecord::getRequestTime, startOfMonth)
                   .le(AiBillingRecord::getRequestTime, now);
            
            List<AiBillingRecord> records = billingRecordMapper.selectList(wrapper);
            BigDecimal monthlyCost = records.stream()
                    .map(r -> r.getCostAmount() != null ? r.getCostAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // 检查告警阈值
            if (monthlyCost.compareTo(BUDGET_CRITICAL_THRESHOLD) >= 0) {
                log.error("BUDGET CRITICAL ALERT: Monthly cost {} exceeds critical threshold {}", 
                         monthlyCost, BUDGET_CRITICAL_THRESHOLD);
                // TODO: 发送告警通知 (邮件/钉钉/企业微信)
            } else if (monthlyCost.compareTo(BUDGET_WARNING_THRESHOLD) >= 0) {
                log.warn("BUDGET WARNING: Monthly cost {} exceeds warning threshold {}", 
                        monthlyCost, BUDGET_WARNING_THRESHOLD);
                // TODO: 发送告警通知
            }
            
        } catch (Exception e) {
            log.error("Failed to check budget alert", e);
        }
    }

    /**
     * 获取预算状态
     */
    private Map<String, Object> getBudgetStatus(BigDecimal currentCost) {
        Map<String, Object> status = new HashMap<>();
        
        String level;
        if (currentCost.compareTo(BUDGET_CRITICAL_THRESHOLD) >= 0) {
            level = "CRITICAL";
        } else if (currentCost.compareTo(BUDGET_WARNING_THRESHOLD) >= 0) {
            level = "WARNING";
        } else {
            level = "NORMAL";
        }
        
        status.put("level", level);
        status.put("currentCost", currentCost.setScale(2, RoundingMode.HALF_UP));
        status.put("warningThreshold", BUDGET_WARNING_THRESHOLD);
        status.put("criticalThreshold", BUDGET_CRITICAL_THRESHOLD);
        
        // 计算使用百分比
        BigDecimal percentage = currentCost.divide(BUDGET_CRITICAL_THRESHOLD, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        status.put("usagePercentage", percentage);
        
        return status;
    }

    /**
     * 获取用户的账单历史
     */
    public List<AiBillingRecord> getUserBillingHistory(Long userId, int limit) {
        // 注意：这里简化处理，实际应该通过conversation关联到user
        LambdaQueryWrapper<AiBillingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AiBillingRecord::getRequestTime)
               .last("LIMIT " + limit);
        return billingRecordMapper.selectList(wrapper);
    }
}
