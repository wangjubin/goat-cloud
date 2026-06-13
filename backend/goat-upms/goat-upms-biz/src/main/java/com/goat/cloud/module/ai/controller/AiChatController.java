package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.model.vo.ChatStreamEvent;
import com.goat.cloud.module.ai.runtime.AiChatStreamService;
import com.goat.cloud.module.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiService aiService;
    private final AiChatStreamService chatStreamService;

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
}
