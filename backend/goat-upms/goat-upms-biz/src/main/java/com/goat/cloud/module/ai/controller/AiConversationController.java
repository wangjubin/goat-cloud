package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiConversation;
import com.goat.cloud.module.ai.entity.AiConversationRecord;
import com.goat.cloud.module.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 会话历史 API
 */
@RestController
@RequestMapping("/api/ai/conversations")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService conversationService;

    /**
     * 获取会话列表
     */
    @PostMapping("/list")
    public ApiResponse<List<AiConversation>> list(@RequestBody Map<String, Object> request) {
        Long agentId = toLong(request.get("agentId"));
        Long userId = toLong(request.get("userId"));
        int pageNum = toInt(request.get("pageNum"), 1);
        int pageSize = toInt(request.get("pageSize"), 20);

        List<AiConversation> conversations = conversationService.listConversations(agentId, userId, pageNum, pageSize);
        return ApiResponse.success(conversations);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{conversationId}")
    public ApiResponse<AiConversation> detail(@PathVariable String conversationId) {
        AiConversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(conversation);
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/{conversationId}/history")
    public ApiResponse<List<AiConversationRecord>> history(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "50") int limit) {
        List<AiConversationRecord> history = conversationService.getHistory(conversationId, limit);
        return ApiResponse.success(history);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception e) { return null; }
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Exception e) { return defaultValue; }
    }
}
