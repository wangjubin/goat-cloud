package com.goat.cloud.module.ai.agent.handler;

import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import com.goat.cloud.module.ai.runtime.AiRuntimeService;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * 上下文收集：组装工具调用、ChatBI、最终 prompt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextCollectorHandler implements AgentChatHandler {

    private final AiRuntimeService runtimeService;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) return;

        // 构建工具调用
        if (!ctx.getTools().isEmpty()) {
            ctx.setToolCalls(buildToolCalls(ctx.getTools(), ctx.getContent()));
            ctx.setToolResults(executeToolCalls(ctx.getToolCalls()));
        }

        // ChatBI 处理
        boolean chatBiEnabled = shouldUseChatBi(ctx.getContent(), ctx.getOptions());
        if (chatBiEnabled) {
            try {
                ChatBiAskRequest chatBiRequest = new ChatBiAskRequest();
                chatBiRequest.setQuestion(ctx.getContent());
                chatBiRequest.setDatasetId(AiRuntimeHelper.toLong(ctx.getOptions().get("chatBiDatasetId")));
                chatBiRequest.setDatasetCode(AiRuntimeHelper.asString(ctx.getOptions().get("chatBiDatasetCode")));
                chatBiRequest.setDatasourceId(AiRuntimeHelper.toLong(ctx.getOptions().get("chatBiDatasourceId")));
                chatBiRequest.setLimit(AiRuntimeHelper.toInteger(ctx.getOptions().get("chatBiLimit"), 100));
                chatBiRequest.setOptions(ctx.getOptions());
                ChatBiAskResponse chatBi = runtimeService.askChatBi(chatBiRequest);
                ctx.setChatBiResponse(chatBi);
                ctx.getToolResults().add(chatBiToolResult(chatBi));
            } catch (Exception ex) {
                log.warn("ChatBI execution failed", ex);
            }
        }

        // 追加工具元数据到 system prompt
        if (!ctx.getTools().isEmpty()) {
            ctx.setSystemPrompt(ctx.getSystemPrompt()
                    + "\n\nAvailable tools: " + ctx.getTools().size()
                    + " (API Skills + MCP Tools). Tool execution is simulated for safety.");
        }

        // 追加 ChatBI 结果到 prompt
        if (ctx.getChatBiResponse() != null) {
            ChatBiAskResponse chatBi = ctx.getChatBiResponse();
            ctx.setSystemPrompt(ctx.getSystemPrompt()
                    + "\n\nChatBI draft SQL: " + (chatBi.getCandidateSql() != null ? chatBi.getCandidateSql() : "N/A")
                    + "\nExecution policy: " + chatBi.getExecutionPolicy());
        }

        ctx.getRuntimeMetadata().put("toolCount", ctx.getTools().size());
        ctx.getRuntimeMetadata().put("historySize", ctx.getHistoryMessages().size());
        ctx.getRuntimeMetadata().put("ragHits", ctx.getRagResponse() != null ? ctx.getRagResponse().getTotal() : 0);

        log.debug("ContextCollector: tools={}, history={}, ragHits={}",
                ctx.getTools().size(), ctx.getHistoryMessages().size(),
                ctx.getRagResponse() != null ? ctx.getRagResponse().getTotal() : 0);
    }

    private List<Map<String, Object>> buildToolCalls(List<Map<String, Object>> tools, String message) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (Map<String, Object> tool : tools) {
            calls.add(Map.of(
                    "toolId", tool.get("id"),
                    "kind", tool.get("kind"),
                    "code", tool.getOrDefault("code", ""),
                    "name", tool.getOrDefault("name", ""),
                    "message", message != null ? message : ""
            ));
        }
        return calls;
    }

    private List<Map<String, Object>> executeToolCalls(List<Map<String, Object>> toolCalls) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> call : toolCalls) {
            results.add(Map.of(
                    "toolId", call.get("toolId"),
                    "kind", call.get("kind"),
                    "status", "SIMULATED",
                    "output", "Tool execution is simulated for safety in this release."
            ));
        }
        return results;
    }

    private boolean shouldUseChatBi(String message, Map<String, Object> options) {
        if (Boolean.TRUE.equals(AiRuntimeHelper.toBoolean(options.get("useChatBi")))) return true;
        String text = AiRuntimeHelper.lower(AiRuntimeHelper.firstText(message, ""));
        return text.contains("count") || text.contains("统计") || text.contains("多少") || text.contains("查询");
    }

    private Map<String, Object> chatBiToolResult(ChatBiAskResponse chatBi) {
        return Map.of(
                "kind", "chatbi",
                "status", chatBi != null ? "SUCCESS" : "SKIPPED",
                "candidateSql", chatBi != null ? chatBi.getCandidateSql() : null,
                "executionPolicy", chatBi != null ? chatBi.getExecutionPolicy() : null
        );
    }

    @Override
    public int order() {
        return 70;
    }
}
