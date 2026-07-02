package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiConversation;
import com.goat.cloud.module.ai.entity.AiConversationRecord;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.model.vo.ChatStreamEvent;
import com.goat.cloud.module.ai.runtime.AiChatStreamService;
import com.goat.cloud.module.ai.service.AiConversationService;
import com.goat.cloud.module.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiService aiService;
    private final AiChatStreamService chatStreamService;
    private final AiConversationService conversationService;

    @PostMapping("/completions")
    public ApiResponse<AiChatResponse> completions(@RequestBody AiChatRequest request) {
        return ApiResponse.success(aiService.chat(request));
    }

    /**
     * SSE 流式聊天端点
     * 返回 SseEmitter，支持实时推送思考过程和文本内容
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时

        String systemPrompt = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
        String userMessage = request.getMessage() != null ? request.getMessage() : "";
        String modelCode = request.getModelCode();
        String apiKeyRef = request.getApiKeyRef();

        // 异步执行流式调用
        new Thread(() -> {
            try {
                chatStreamService.chatStream(systemPrompt, userMessage, modelCode, apiKeyRef, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(ChatStreamEvent.error("Stream failed: " + e.getMessage()), MediaType.APPLICATION_JSON);
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();

        emitter.onTimeout(() -> emitter.complete());
        emitter.onError(t -> emitter.complete());

        return emitter;
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public ApiResponse<List<AiConversation>> listConversations(
            @RequestParam Long agentId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<AiConversation> conversations = conversationService.listConversations(agentId, userId, pageNum, pageSize);
        return ApiResponse.success(conversations);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<AiConversation> getConversation(@PathVariable String conversationId) {
        AiConversation conversation = conversationService.getConversation(conversationId);
        return ApiResponse.success(conversation);
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<AiConversationRecord>> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "50") int limit) {
        List<AiConversationRecord> messages = conversationService.getHistory(conversationId, limit);
        return ApiResponse.success(messages);
    }

    /**
     * 删除会话（软删除）
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> deleteConversation(@PathVariable String conversationId) {
        conversationService.deleteConversation(conversationId);
        return ApiResponse.success();
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/conversations/{conversationId}/title")
    public ApiResponse<Void> updateConversationTitle(
            @PathVariable String conversationId,
            @RequestParam String title) {
        conversationService.updateConversationTitle(conversationId, title);
        return ApiResponse.success();
    }
}
