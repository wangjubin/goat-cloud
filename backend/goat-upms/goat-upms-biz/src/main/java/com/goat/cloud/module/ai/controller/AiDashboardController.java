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
import lombok.RequiredArgsConstructor;
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

/**
 * Dashboard 统计 API
 */
@RestController
@RequestMapping("/api/ai/dashboard")
@RequiredArgsConstructor
public class AiDashboardController {

    private final AiModelConfigMapper modelConfigMapper;
    private final AiAgentMapper agentMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiDocumentMapper documentMapper;
    private final AiConversationMapper conversationMapper;
    private final AiConversationRecordMapper recordMapper;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 基础统计
        stats.put("modelCount", countNonNull(modelConfigMapper));
        stats.put("agentCount", countNonNull(agentMapper));
        stats.put("knowledgeBaseCount", countNonNull(knowledgeBaseMapper));
        stats.put("documentCount", countNonNull(documentMapper));
        stats.put("conversationCount", countNonNull(conversationMapper));
        stats.put("messageCount", countNonNull(recordMapper));

        // 今日统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        stats.put("todayConversations", countToday(conversationMapper, todayStart, todayEnd));
        stats.put("todayMessages", countToday(recordMapper, todayStart, todayEnd));

        // 最近 7 天趋势
        stats.put("dailyTrend", buildDailyTrend());

        return ApiResponse.success(stats);
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        overview.put("platform", "Goat Cloud AI Platform");
        overview.put("version", "1.0.0");
        overview.put("modules", List.of(
            Map.of("name", "Model Management", "status", "active", "count", countNonNull(modelConfigMapper)),
            Map.of("name", "Agent Orchestration", "status", "active", "count", countNonNull(agentMapper)),
            Map.of("name", "RAG Knowledge Base", "status", "active", "count", countNonNull(knowledgeBaseMapper)),
            Map.of("name", "Document Processing", "status", "active", "count", countNonNull(documentMapper)),
            Map.of("name", "Chat & Conversation", "status", "active", "count", countNonNull(conversationMapper))
        ));

        return ApiResponse.success(overview);
    }

    private long countNonNull(Object mapper) {
        try {
            if (mapper instanceof com.baomidou.mybatisplus.core.mapper.BaseMapper<?> baseMapper) {
                return baseMapper.selectCount(null);
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    private long countToday(Object mapper, LocalDateTime start, LocalDateTime end) {
        try {
            if (mapper instanceof AiConversationMapper convMapper) {
                return convMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversation>()
                        .between(AiConversation::getCreateTime, start, end));
            }
            if (mapper instanceof AiConversationRecordMapper recordMapper) {
                return recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversationRecord>()
                        .between(AiConversationRecord::getCreateTime, start, end));
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    private List<Map<String, Object>> buildDailyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            long conversations = 0;
            long messages = 0;
            try {
                conversations = conversationMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversation>()
                        .between(AiConversation::getCreateTime, dayStart, dayEnd));
                messages = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversationRecord>()
                        .between(AiConversationRecord::getCreateTime, dayStart, dayEnd));
            } catch (Exception e) {
                // ignore
            }

            trend.add(Map.of(
                    "date", date.toString(),
                    "conversations", conversations,
                    "messages", messages
            ));
        }
        return trend;
    }
}
