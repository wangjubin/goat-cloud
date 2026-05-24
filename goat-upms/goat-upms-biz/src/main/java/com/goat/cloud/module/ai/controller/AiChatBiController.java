package com.goat.cloud.module.ai.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.framework.security.CurrentUserHolder;
import com.goat.cloud.module.ai.entity.AiStateSession;
import com.goat.cloud.module.ai.service.stategraph.StateExecutionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ChatBI 智能问数控制器
 * <p>
 * 核心入口：用户提问 → StateGraph 工作流执行 → SSE 流式返回结果
 * <p>
 * SSE 增强特性：
 * - 心跳保活（15秒间隔）
 * - 异步执行 + 超时控制（120秒）
 * - 客户端断开检测
 * - 连接异常自动清理
 * @author wangjubin
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chatbi")
@RequiredArgsConstructor
@EnableAsync
public class AiChatBiController {

    private final StateExecutionEngine executionEngine;
    private final ObjectMapper objectMapper;

    private static final long SSE_TIMEOUT_MS = 120_000;
    private static final long HEARTBEAT_INTERVAL_MS = 15_000;

    /**
     * 流式对话（SSE）
     * <p>
     * 用户提问后，通过 SSE 逐步返回各节点执行结果：
     * - 意图识别结果
     * - Schema 召回结果
     * - 生成的 SQL
     * - SQL 执行结果
     * - 图表配置（ECharts）
     * - 心跳保活
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void chatStream(@RequestBody ChatRequest request, HttpServletResponse response) throws IOException {
        Long userId = CurrentUserHolder.require().getUserId();
        configureSseResponse(response);
        PrintWriter writer = response.getWriter();
        AtomicBoolean clientConnected = new AtomicBoolean(true);

        // Start heartbeat in background
        ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (clientConnected.get()) {
                sendSse(writer, "heartbeat", Map.of("timestamp", System.currentTimeMillis()));
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Execute workflow asynchronously with timeout
        ExecutorService workflowExecutor = Executors.newSingleThreadExecutor();
        Future<AiStateSession> future = workflowExecutor.submit(() ->
                executionEngine.executeStreaming(
                        request.graphCode() != null ? request.graphCode() : "chatbi_default",
                        userId,
                        Map.of("question", request.question(), "datasourceId", request.datasourceId()),
                        new SseCallbackAdapter(writer, objectMapper)
                )
        );

        try {
            AiStateSession session = future.get(SSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            sendSse(writer, "complete", Map.of(
                    "runId", session.getRunId(),
                    "status", session.getStatus(),
                    "result", session.getResultJson()
            ));
        } catch (TimeoutException e) {
            log.warn("ChatBI stream timeout for question: {}", request.question());
            future.cancel(true);
            sendSse(writer, "timeout", Map.of("message", "执行超时，请简化问题或稍后重试"));
        } catch (ExecutionException e) {
            log.error("ChatBI stream execution error", e.getCause());
            sendSse(writer, "error", Map.of("message", e.getCause() != null ? e.getCause().getMessage() : "执行失败"));
        } catch (InterruptedException e) {
            log.warn("ChatBI stream interrupted");
            Thread.currentThread().interrupt();
            sendSse(writer, "error", Map.of("message", "执行被中断"));
        } finally {
            clientConnected.set(false);
            heartbeatExecutor.shutdownNow();
            workflowExecutor.shutdownNow();
            try {
                heartbeatExecutor.awaitTermination(2, TimeUnit.SECONDS);
                workflowExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            writer.flush();
        }
    }

    /**
     * 非流式对话（直接返回完整结果）
     */
    @PostMapping("/chat")
    public ApiResponse<AiStateSession> chat(@RequestBody ChatRequest request) {
        Long userId = CurrentUserHolder.require().getUserId();
        AiStateSession session = executionEngine.startSession(
                request.graphCode() != null ? request.graphCode() : "chatbi_default",
                userId,
                Map.of("question", request.question(), "datasourceId", request.datasourceId())
        );
        return ApiResponse.success(session);
    }

    /**
     * 恢复中断的会话（人工反馈后继续）
     */
    @PostMapping("/chat/resume")
    public ApiResponse<AiStateSession> resumeSession(@RequestBody ResumeRequest request) {
        Long userId = CurrentUserHolder.require().getUserId();
        AiStateSession session = executionEngine.resumeSession(request.runId(), request.feedback());
        return ApiResponse.success(session);
    }

    /**
     * 查询会话状态
     */
    @GetMapping("/chat/session/{runId}")
    public ApiResponse<AiStateSession> getSession(@PathVariable String runId) {
        return ApiResponse.success(executionEngine.getSession(runId));
    }

    /**
     * 取消会话
     */
    @PostMapping("/chat/cancel/{runId}")
    public ApiResponse<Void> cancelSession(@PathVariable String runId) {
        executionEngine.cancelSession(runId);
        return ApiResponse.success();
    }

    /**
     * 获取会话追踪记录
     */
    @GetMapping("/chat/traces/{sessionId}")
    public ApiResponse<Object> getTraces(@PathVariable Long sessionId) {
        return ApiResponse.success(executionEngine.getSessionTraces(sessionId));
    }

    // ========== Helpers ==========

    private void configureSseResponse(HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void sendSse(PrintWriter writer, String event, Object data) {
        try {
            writer.write("event: " + event + "\n");
            writer.write("data: " + objectMapper.writeValueAsString(data) + "\n\n");
            writer.flush();
        } catch (Exception e) {
            log.warn("SSE write error for event {}: {}", event, e.getMessage());
        }
    }

    // ========== SSE Callback Adapter ==========

    private static class SseCallbackAdapter implements StateExecutionEngine.StreamCallback {
        private final PrintWriter writer;
        private final ObjectMapper mapper;

        SseCallbackAdapter(PrintWriter writer, ObjectMapper mapper) {
            this.writer = writer;
            this.mapper = mapper;
        }

        @Override
        public void onNodeStart(String nodeCode, String nodeType) {
            send("node_start", Map.of("nodeCode", nodeCode, "nodeType", nodeType));
        }

        @Override
        public void onNodeComplete(String nodeCode, String nodeType, String outputJson) {
            send("node_complete", Map.of("nodeCode", nodeCode, "nodeType", nodeType, "output", outputJson));
        }

        @Override
        public void onNodeError(String nodeCode, String nodeType, String errorMessage) {
            send("node_error", Map.of("nodeCode", nodeCode, "nodeType", nodeType, "error", errorMessage));
        }

        @Override
        public void onInterrupt(String nodeCode, String reason) {
            send("interrupt", Map.of("nodeCode", nodeCode, "reason", reason));
        }

        @Override
        public void onComplete(AiStateSession session) {
            // Complete event is sent by the controller after future.get()
        }

        private void send(String event, Object data) {
            try {
                writer.write("event: " + event + "\n");
                writer.write("data: " + mapper.writeValueAsString(data) + "\n\n");
                writer.flush();
            } catch (Exception e) {
                // Client may have disconnected
            }
        }
    }

    // ========== Request DTOs ==========

    public record ChatRequest(
            String question,
            Long datasourceId,
            String graphCode
    ) {}

    public record ResumeRequest(
            String runId,
            Map<String, Object> feedback
    ) {}
}