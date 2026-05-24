package com.goat.cloud.module.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.framework.security.CurrentUserHolder;
import com.goat.cloud.module.ai.entity.AiStateSession;
import com.goat.cloud.module.ai.entity.AiStateTrace;
import com.goat.cloud.module.ai.mapper.AiStateSessionMapper;
import com.goat.cloud.module.ai.mapper.AiStateTraceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatBI 对话历史控制器
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chatbi/history")
@RequiredArgsConstructor
public class AiChatBiHistoryController {

    private final AiStateSessionMapper sessionMapper;
    private final AiStateTraceMapper traceMapper;

    /**
     * 获取当前用户的对话历史列表
     */
    @GetMapping("/sessions")
    public ApiResponse<List<AiStateSession>> listSessions() {
        Long userId = CurrentUserHolder.require().getUserId();
        List<AiStateSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AiStateSession>()
                        .eq(AiStateSession::getUserId, userId)
                        .orderByDesc(AiStateSession::getStartedAt)
                        .last("limit 50")
        );
        return ApiResponse.success(sessions);
    }

    /**
     * 获取某次对话的完整追踪详情
     */
    @GetMapping("/sessions/{sessionId}/traces")
    public ApiResponse<List<AiStateTrace>> getSessionTraces(@PathVariable Long sessionId) {
        return ApiResponse.success(traceMapper.selectList(
                new LambdaQueryWrapper<AiStateTrace>()
                        .eq(AiStateTrace::getSessionId, sessionId)
                        .orderByAsc(AiStateTrace::getStartedAt)
        ));
    }

    /**
     * 获取对话统计
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Long userId = CurrentUserHolder.require().getUserId();
        long totalSessions = sessionMapper.selectCount(
                new LambdaQueryWrapper<AiStateSession>().eq(AiStateSession::getUserId, userId)
        );
        long completedSessions = sessionMapper.selectCount(
                new LambdaQueryWrapper<AiStateSession>()
                        .eq(AiStateSession::getUserId, userId)
                        .eq(AiStateSession::getStatus, "COMPLETED")
        );
        long failedSessions = sessionMapper.selectCount(
                new LambdaQueryWrapper<AiStateSession>()
                        .eq(AiStateSession::getUserId, userId)
                        .eq(AiStateSession::getStatus, "FAILED")
        );

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSessions", totalSessions);
        stats.put("completedSessions", completedSessions);
        stats.put("failedSessions", failedSessions);
        return ApiResponse.success(stats);
    }
}