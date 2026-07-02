package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiConversation;
import com.goat.cloud.module.ai.entity.AiConversationRecord;
import com.goat.cloud.module.ai.mapper.AiAgentMapper;
import com.goat.cloud.module.ai.mapper.AiConversationMapper;
import com.goat.cloud.module.ai.mapper.AiConversationRecordMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentMapper;
import com.goat.cloud.module.ai.mapper.AiKnowledgeBaseMapper;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.service.AiBillingService;
import com.goat.cloud.module.ai.service.AiRequestLogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class AiDashboardController {

    private final AiModelConfigMapper modelConfigMapper;
    private final AiAgentMapper agentMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiDocumentMapper documentMapper;
    private final AiConversationMapper conversationMapper;
    private final AiConversationRecordMapper recordMapper;
    private final AiRequestLogService requestLogService;
    private final AiBillingService billingService;

    @GetMapping("/stats")
    public ApiResponse<Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            stats.put("modelCount", safeCount(modelConfigMapper));
            stats.put("agentCount", safeCount(agentMapper));
            stats.put("knowledgeBaseCount", safeCount(knowledgeBaseMapper));
            stats.put("documentCount", safeCount(documentMapper));
            stats.put("conversationCount", safeCount(conversationMapper));
            stats.put("messageCount", safeCount(recordMapper));

            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            stats.put("todayConversations", countConvBetween(todayStart, todayEnd));
            stats.put("todayMessages", countRecordBetween(todayStart, todayEnd));
            
            // Get today's request log statistics
            Map<String, Object> todayStats = requestLogService.getTimeRangeStats(todayStart, todayEnd);
            stats.put("todayRequests", todayStats.getOrDefault("totalRequests", 0L));
            stats.put("todayTokens", todayStats.getOrDefault("totalTokens", 0L));
            stats.put("successRate", todayStats.getOrDefault("successRate", 0.0));
            stats.put("avgLatencyMs", todayStats.getOrDefault("avgLatencyMs", 0L));
            
            // Get today's cost statistics
            Map<String, Object> costStats = billingService.getCostStats(todayStart, todayEnd);
            stats.put("todayCost", costStats.getOrDefault("totalCost", 0.0));
            
            stats.put("dailyTrend", buildDailyTrend());
        } catch (Exception e) {
            log.error("Dashboard stats error", e);
            putDefaults(stats);
        }
        return ApiResponse.success(stats);
    }

    @GetMapping("/overview")
    public ApiResponse<Object> overview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("platform", "Goat Cloud AI Platform");
        overview.put("version", "1.0.0");
        overview.put("modules", List.of(
            Map.of("name", "模型管理", "status", "active", "count", safeCount(modelConfigMapper)),
            Map.of("name", "智能体编排", "status", "active", "count", safeCount(agentMapper)),
            Map.of("name", "RAG 知识库", "status", "active", "count", safeCount(knowledgeBaseMapper)),
            Map.of("name", "文档处理", "status", "active", "count", safeCount(documentMapper)),
            Map.of("name", "对话管理", "status", "active", "count", safeCount(conversationMapper))
        ));
        return ApiResponse.success(overview);
    }

    private long safeCount(com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper) {
        try {
            return mapper.selectCount(new QueryWrapper<>());
        } catch (Exception e) {
            return 0;
        }
    }

    private long countConvBetween(LocalDateTime start, LocalDateTime end) {
        try {
            return conversationMapper.selectCount(
                    new QueryWrapper<AiConversation>().between("create_time", start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private long countRecordBetween(LocalDateTime start, LocalDateTime end) {
        try {
            return recordMapper.selectCount(
                    new QueryWrapper<AiConversationRecord>().between("create_time", start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Map<String, Object>> buildDailyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("conversations", countConvBetween(dayStart, dayEnd));
            day.put("messages", countRecordBetween(dayStart, dayEnd));
            trend.add(day);
        }
        return trend;
    }

    private void putDefaults(Map<String, Object> stats) {
        stats.putIfAbsent("modelCount", 0L);
        stats.putIfAbsent("agentCount", 0L);
        stats.putIfAbsent("knowledgeBaseCount", 0L);
        stats.putIfAbsent("documentCount", 0L);
        stats.putIfAbsent("conversationCount", 0L);
        stats.putIfAbsent("messageCount", 0L);
        stats.putIfAbsent("todayConversations", 0L);
        stats.putIfAbsent("todayMessages", 0L);
        stats.putIfAbsent("dailyTrend", List.of());
    }
}
