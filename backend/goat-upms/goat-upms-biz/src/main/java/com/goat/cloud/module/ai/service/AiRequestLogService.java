package com.goat.cloud.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.module.ai.entity.AiRequestLog;
import com.goat.cloud.module.ai.mapper.AiRequestLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI请求日志服务
 * 负责记录、查询和统计AI请求日志
 * 
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRequestLogService {

    private final AiRequestLogMapper requestLogMapper;

    /**
     * 创建请求日志
     */
    public String createLog(Long userId, Long modelId, String modelName, String provider,
                           String bizType, String conversationId, Boolean stream) {
        AiRequestLog requestLog = new AiRequestLog();
        requestLog.setUserId(userId);
        requestLog.setModelId(modelId);
        requestLog.setModelName(modelName);
        requestLog.setProvider(provider);
        requestLog.setBizType(bizType);
        requestLog.setConversationId(conversationId);
        requestLog.setStream(stream != null ? stream : false);
        requestLog.setRequestId(UUID.randomUUID().toString());
        requestLog.setRequestTime(LocalDateTime.now());
        requestLog.setStatus("IN_PROGRESS");
        
        requestLogMapper.insert(requestLog);
        log.debug("Created request log: requestId={}, model={}", requestLog.getRequestId(), modelName);
        
        return requestLog.getRequestId();
    }

    /**
     * 更新请求日志(请求完成时调用)
     */
    public void completeLog(String requestId, Integer promptTokens, Integer completionTokens,
                           Long latencyMs, String status, String errorMessage) {
        if (!StringUtils.hasText(requestId)) {
            log.warn("Cannot complete log: requestId is empty");
            return;
        }
        
        LambdaQueryWrapper<AiRequestLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRequestLog::getRequestId, requestId);
        AiRequestLog requestLog = requestLogMapper.selectOne(wrapper);
        
        if (requestLog == null) {
            log.warn("Request log not found: requestId={}", requestId);
            return;
        }
        
        requestLog.setPromptTokens(promptTokens != null ? promptTokens : 0);
        requestLog.setCompletionTokens(completionTokens != null ? completionTokens : 0);
        requestLog.setTotalTokens((promptTokens != null ? promptTokens : 0) + 
                                  (completionTokens != null ? completionTokens : 0));
        requestLog.setLatencyMs(latencyMs);
        requestLog.setStatus(status);
        requestLog.setErrorMessage(errorMessage);
        requestLog.setResponseTime(LocalDateTime.now());
        
        requestLogMapper.updateById(requestLog);
        log.debug("Completed request log: requestId={}, status={}, tokens={}", 
                  requestId, status, requestLog.getTotalTokens());
    }

    /**
     * 获取今日统计
     */
    public Map<String, Object> getTodayStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        return getTimeRangeStats(startOfDay, endOfDay);
    }

    /**
     * 获取指定时间范围的统计
     */
    public Map<String, Object> getTimeRangeStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 请求总数
            Long totalRequests = requestLogMapper.countByTimeRange(startTime, endTime);
            stats.put("totalRequests", totalRequests);
            
            // Token消耗
            Map<String, Long> tokenStats = requestLogMapper.sumTokensByTimeRange(startTime, endTime);
            stats.put("promptTokens", tokenStats.getOrDefault("prompt_tokens", 0L));
            stats.put("completionTokens", tokenStats.getOrDefault("completion_tokens", 0L));
            stats.put("totalTokens", tokenStats.getOrDefault("total_tokens", 0L));
            
            // 成功率
            Map<String, Long> successStats = requestLogMapper.countSuccessRate(startTime, endTime);
            Long total = successStats.getOrDefault("total", 0L);
            Long success = successStats.getOrDefault("success_count", 0L);
            double successRate = total > 0 ? (success * 100.0 / total) : 0;
            stats.put("successRate", Math.round(successRate * 100) / 100.0);
            stats.put("failedCount", successStats.getOrDefault("failed_count", 0L));
            
            // 响应时间
            Map<String, Long> latencyStats = requestLogMapper.avgLatency(startTime, endTime);
            stats.put("avgLatencyMs", latencyStats.getOrDefault("avg_latency", 0L));
            stats.put("maxLatencyMs", latencyStats.getOrDefault("max_latency", 0L));
            stats.put("minLatencyMs", latencyStats.getOrDefault("min_latency", 0L));
            
            // 按模型分组统计
            List<Map<String, Object>> modelStats = requestLogMapper.countByModel(startTime, endTime);
            stats.put("modelStats", modelStats);
            
            // Top用户
            List<Map<String, Object>> topUsers = requestLogMapper.countByUser(startTime, endTime, 10);
            stats.put("topUsers", topUsers);
            
        } catch (Exception e) {
            log.error("Failed to get time range stats", e);
            stats.put("error", "获取统计失败: " + e.getMessage());
        }
        
        return stats;
    }

    /**
     * 获取用户的请求历史
     */
    public List<AiRequestLog> getUserRequestHistory(Long userId, int limit) {
        LambdaQueryWrapper<AiRequestLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRequestLog::getUserId, userId)
               .orderByDesc(AiRequestLog::getRequestTime)
               .last("LIMIT " + limit);
        return requestLogMapper.selectList(wrapper);
    }

    /**
     * 获取会话的请求历史
     */
    public List<AiRequestLog> getConversationRequestHistory(String conversationId) {
        LambdaQueryWrapper<AiRequestLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRequestLog::getConversationId, conversationId)
               .orderByAsc(AiRequestLog::getRequestTime);
        return requestLogMapper.selectList(wrapper);
    }

    /**
     * 根据请求ID获取日志
     */
    public AiRequestLog getByRequestId(String requestId) {
        LambdaQueryWrapper<AiRequestLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRequestLog::getRequestId, requestId);
        return requestLogMapper.selectOne(wrapper);
    }
}
