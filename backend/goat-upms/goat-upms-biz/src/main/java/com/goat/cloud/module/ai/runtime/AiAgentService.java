package com.goat.cloud.module.ai.runtime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.module.ai.agent.AgentChatChain;
import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.entity.AiAgent;
import com.goat.cloud.module.ai.entity.AiApiSkill;
import com.goat.cloud.module.ai.entity.AiMcpTool;
import com.goat.cloud.module.ai.entity.AiPromptTemplate;
import com.goat.cloud.module.ai.mapper.AiAgentMapper;
import com.goat.cloud.module.ai.mapper.AiApiSkillMapper;
import com.goat.cloud.module.ai.mapper.AiMcpToolMapper;
import com.goat.cloud.module.ai.mapper.AiPromptTemplateMapper;
import com.goat.cloud.module.ai.memory.ShortTermMessage;
import com.goat.cloud.module.ai.memory.ShortTermMemoryStore;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.runtime.model.AgentRunRequest;
import com.goat.cloud.module.ai.runtime.model.AgentRunResponse;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse;
import com.goat.cloud.module.ai.runtime.model.RagSearchRequest;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import com.goat.cloud.module.ai.runtime.model.RagSearchHit;
import com.goat.cloud.module.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiAgentService {

    private static final int DEFAULT_RAG_TOP_K = 5;
    private static final int DEFAULT_SHORT_TERM_WINDOW = 20;

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final AiAgentMapper agentMapper;
    private final AiPromptTemplateMapper promptTemplateMapper;
    private final AiApiSkillMapper apiSkillMapper;
    private final AiMcpToolMapper mcpToolMapper;
    private final AiRagSearchService ragSearchService;
    private final AiChatBiService chatBiService;
    private final AiChatService chatService;
    private final AiConversationService conversationService;
    private final ShortTermMemoryStore shortTermMemoryStore;
    private final AgentChatChain agentChatChain;

    public AgentRunResponse runAgent(Long agentId, AgentRunRequest request) {
        if (agentId == null) {
            throw new BusinessException(4001, "Agent id is required");
        }
        AiAgent agent = safeSelectById(agentMapper, agentId);
        if (agent == null) {
            throw new BusinessException(4044, "Agent not found");
        }
        AgentRunRequest safeRequest = request == null ? new AgentRunRequest() : request;
        AiPromptTemplate prompt = agent.getPromptId() == null ? null : safeSelectById(promptTemplateMapper, agent.getPromptId());
        List<Long> knowledgeBaseIds = parseLongList(agent.getKnowledgeBaseIds());
        List<Map<String, Object>> tools = resolveAgentTools(agent.getToolIds());
        Map<String, Object> options = safeOptions(safeRequest.getOptions());
        boolean ragEnabled = !knowledgeBaseIds.isEmpty() && isRagEnabled(options);
        boolean toolingEnabled = !tools.isEmpty() && !Boolean.FALSE.equals(AiRuntimeHelper.toBoolean(options.get("useTools")));
        boolean chatBiEnabled = shouldUseChatBi(safeRequest.getMessage(), options);

        // 加载短期记忆历史
        String conversationId = StringUtils.hasText(safeRequest.getConversationId())
                ? safeRequest.getConversationId() : UUID.randomUUID().toString();
        int windowSize = AiRuntimeHelper.toInteger(options.get("shortTermWindow"), DEFAULT_SHORT_TERM_WINDOW);
        List<ShortTermMessage> historyMessages = shortTermMemoryStore.loadHistory(conversationId, windowSize);

        // 保存用户消息到短期记忆和持久化
        if (StringUtils.hasText(safeRequest.getMessage())) {
            shortTermMemoryStore.append(conversationId, "user", safeRequest.getMessage(), windowSize);
            try {
                Long userId = AiRuntimeHelper.toLong(options.get("userId"));
                if (userId != null) {
                    conversationService.getOrCreateConversation(conversationId, agentId, userId, safeRequest.getMessage());
                    conversationService.saveMessage(conversationId, agentId, userId, "user", safeRequest.getMessage(), null);
                }
            } catch (Exception ex) {
                // 对话持久化失败不阻塞主流程
            }
        }

        List<Map<String, Object>> plan = buildAgentRunPlan(agent, safeRequest, knowledgeBaseIds, tools, ragEnabled, toolingEnabled, chatBiEnabled);

        RagSearchResponse rag = null;
        if (ragEnabled && StringUtils.hasText(safeRequest.getMessage())) {
            RagSearchRequest ragRequest = new RagSearchRequest();
            ragRequest.setQuery(safeRequest.getMessage());
            ragRequest.setTopK(AiRuntimeHelper.toInteger(options.get("topK"), DEFAULT_RAG_TOP_K));
            ragRequest.setIncludeContent(Boolean.TRUE);
            ragRequest.setKnowledgeBaseIds(knowledgeBaseIds);
            rag = ragSearchService.search(ragRequest);
        }

        List<Map<String, Object>> toolCalls = toolingEnabled ? buildToolCalls(tools, safeRequest) : List.of();
        List<Map<String, Object>> toolResults = executeAgentTools(toolCalls);

        ChatBiAskResponse chatBi = null;
        if (chatBiEnabled) {
            ChatBiAskRequest chatBiRequest = new ChatBiAskRequest();
            chatBiRequest.setQuestion(safeRequest.getMessage());
            chatBiRequest.setDatasetId(AiRuntimeHelper.toLong(options.get("chatBiDatasetId")));
            chatBiRequest.setDatasetCode(AiRuntimeHelper.asString(options.get("chatBiDatasetCode")));
            chatBiRequest.setDatasourceId(AiRuntimeHelper.toLong(options.get("chatBiDatasourceId")));
            chatBiRequest.setLimit(AiRuntimeHelper.toInteger(options.get("chatBiLimit"), 100));
            chatBiRequest.setOptions(options);
            chatBi = chatBiService.askChatBi(chatBiRequest);
            toolResults = appendToolResult(toolResults, chatBiToolResult(chatBi));
        }

        AiChatRequest chatRequest = new AiChatRequest();
        chatRequest.setConversationId(conversationId);
        chatRequest.setModelId(agent.getModelId());
        chatRequest.setMessage(safeRequest.getMessage());
        chatRequest.setSystemPrompt(buildAgentRuntimePrompt(agent, prompt, tools, plan, rag, toolResults, chatBi));
        options.put("bizType", "AGENT");
        options.put("agentId", agent.getAgentId());
        if (!knowledgeBaseIds.isEmpty()) {
            options.put("knowledgeBaseIds", knowledgeBaseIds);
        }
        options.put("rag", Boolean.FALSE);
        chatRequest.setOptions(options);
        AiChatResponse chat = chatService.chat(chatRequest);

        // 保存助手回复到短期记忆和持久化
        if (chat.getMessage() != null && StringUtils.hasText(chat.getMessage().getContent())) {
            shortTermMemoryStore.append(conversationId, "assistant", chat.getMessage().getContent(), windowSize);
            try {
                Long userId = AiRuntimeHelper.toLong(options.get("userId"));
                if (userId != null) {
                    conversationService.saveMessage(conversationId, agentId, userId, "assistant", chat.getMessage().getContent(), null);
                }
            } catch (Exception ex) {
                // 对话持久化失败不阻塞主流程
            }
        }

        AgentRunResponse response = new AgentRunResponse();
        response.setAgentId(agent.getAgentId());
        response.setAgentCode(agent.getAgentCode());
        response.setAgentName(agent.getAgentName());
        response.setDescription(agent.getDescription());
        response.setModelId(agent.getModelId());
        response.setPromptId(agent.getPromptId());
        response.setKnowledgeBaseIds(knowledgeBaseIds);
        response.setTools(tools);
        response.setChat(chat);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("plan", plan);
        metadata.put("toolCalls", toolCalls);
        metadata.put("toolResults", toolResults);
        metadata.put("rag", agentRagMetadata(rag, ragEnabled, knowledgeBaseIds));
        metadata.put("chatBi", agentChatBiMetadata(chatBi, chatBiEnabled));
        metadata.put("memory", Map.of("config", parseJson(agent.getMemoryConfig()), "conversationId", chat.getConversationId()));
        metadata.put("runtime", Map.of(
                "mode", "agent-plan-rag-tools-chatbi-chat",
                "safety", "Only local GET /actuator/health can be called; all other API/MCP tools are simulated or skipped.",
                "providerFinishReason", chat.getFinishReason(),
                "mock", chat.getMock()
        ));
        response.setMetadata(metadata);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    /**
     * 使用责任链模式运行 Agent（新架构）
     * 参考 snail-ai 的 handler chain 模式
     */
    public AgentRunResponse runAgentWithChain(Long agentId, AgentRunRequest request) {
        if (agentId == null) {
            throw new BusinessException(4001, "Agent id is required");
        }
        AgentRunRequest safeRequest = request == null ? new AgentRunRequest() : request;
        Map<String, Object> options = safeOptions(safeRequest.getOptions());
        String conversationId = StringUtils.hasText(safeRequest.getConversationId())
                ? safeRequest.getConversationId() : java.util.UUID.randomUUID().toString();
        Long userId = AiRuntimeHelper.toLong(options.get("userId"));

        // 构建上下文
        AgentChatContext ctx = new AgentChatContext(agentId, conversationId,
                safeRequest.getMessage(), userId, options);

        // 执行 handler 链
        agentChatChain.execute(ctx);

        // 如果链路被终止，抛出异常
        if (ctx.isTerminated()) {
            throw new BusinessException(5000, ctx.getErrorMessage());
        }

        // 从上下文构建响应
        AiChatResponse chat = (AiChatResponse) ctx.getRuntimeMetadata().get("chatResponse");

        AgentRunResponse response = new AgentRunResponse();
        response.setAgentId(ctx.getAgent().getAgentId());
        response.setAgentCode(ctx.getAgent().getAgentCode());
        response.setAgentName(ctx.getAgent().getAgentName());
        response.setDescription(ctx.getAgent().getDescription());
        response.setModelId(ctx.getAgent().getModelId());
        response.setPromptId(ctx.getAgent().getPromptId());
        response.setKnowledgeBaseIds(parseLongList(ctx.getAgent().getKnowledgeBaseIds()));
        response.setTools(ctx.getTools());
        response.setChat(chat);

        // 构建 metadata
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("tools", ctx.getTools());
        metadata.put("toolCalls", ctx.getToolCalls());
        metadata.put("toolResults", ctx.getToolResults());
        metadata.put("historySize", ctx.getHistoryMessages().size());
        metadata.put("ragHits", ctx.getRagResponse() != null ? ctx.getRagResponse().getTotal() : 0);
        metadata.put("chatBi", ctx.getChatBiResponse() != null ? Map.of(
                "candidateSql", ctx.getChatBiResponse().getCandidateSql(),
                "executionPolicy", ctx.getChatBiResponse().getExecutionPolicy()
        ) : null);
        metadata.put("runtime", Map.of(
                "mode", "agent-handler-chain",
                "finishReason", ctx.getRuntimeMetadata().getOrDefault("finishReason", "unknown"),
                "mock", ctx.getRuntimeMetadata().getOrDefault("mock", false)
        ));
        response.setMetadata(metadata);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private List<Map<String, Object>> buildAgentRunPlan(AiAgent agent, AgentRunRequest request, List<Long> knowledgeBaseIds,
                                                        List<Map<String, Object>> tools, boolean ragEnabled,
                                                        boolean toolingEnabled, boolean chatBiEnabled) {
        List<Map<String, Object>> plan = new ArrayList<>();
        plan.add(planStep("understand", true, "Analyze the user message, agent role, variables, and memory policy.",
                Map.of("messagePreview", AiRuntimeHelper.preview(request.getMessage(), 160), "hasVariables", request.getVariables() != null && !request.getVariables().isEmpty())));
        plan.add(planStep("retrieve", ragEnabled, ragEnabled
                        ? "Retrieve grounded context from the agent knowledge bases."
                        : "Skipped because the agent has no knowledge bases or RAG was disabled.",
                Map.of("knowledgeBaseIds", knowledgeBaseIds)));
        plan.add(planStep("tooling", toolingEnabled || chatBiEnabled, toolingEnabled || chatBiEnabled
                        ? "Prepare safe tool calls; only local health-check GET can execute, other tools are simulated."
                        : "Skipped because the agent has no tools and ChatBI was not requested.",
                Map.of("toolCount", tools.size(), "chatBiRequested", chatBiEnabled)));
        plan.add(planStep("answer", true, "Generate the final answer with the plan, citations, tool results, and ChatBI draft SQL injected into the system prompt.",
                Map.of("agentId", agent.getAgentId(), "agentCode", agent.getAgentCode())));
        return plan;
    }

    private Map<String, Object> planStep(String type, boolean enabled, String rationale, Map<String, Object> inputs) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("type", type);
        step.put("enabled", enabled);
        step.put("status", enabled ? "READY" : "SKIPPED");
        step.put("rationale", rationale);
        step.put("inputs", inputs);
        return step;
    }

    private boolean shouldUseChatBi(String message, Map<String, Object> options) {
        if (Boolean.TRUE.equals(AiRuntimeHelper.toBoolean(options.get("useChatBi")))) return true;
        String text = AiRuntimeHelper.lower(AiRuntimeHelper.firstText(message, ""));
        return text.contains("count") || text.contains("统计") || text.contains("多少") || text.contains("查询");
    }

    private List<Map<String, Object>> buildToolCalls(List<Map<String, Object>> tools, AgentRunRequest request) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (Map<String, Object> tool : tools) {
            Map<String, Object> call = new LinkedHashMap<>();
            call.put("callId", UUID.randomUUID().toString());
            call.put("kind", tool.get("kind"));
            call.put("toolId", tool.get("id"));
            call.put("code", tool.get("code"));
            call.put("name", tool.get("name"));
            call.put("method", tool.get("method"));
            call.put("endpoint", tool.get("endpoint"));
            call.put("arguments", Map.of(
                    "message", AiRuntimeHelper.firstText(request.getMessage(), ""),
                    "variables", request.getVariables() == null ? Map.of() : request.getVariables()
            ));
            calls.add(call);
        }
        return calls;
    }

    private List<Map<String, Object>> executeAgentTools(List<Map<String, Object>> toolCalls) {
        if (toolCalls.isEmpty()) return List.of();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> call : toolCalls) {
            results.add(executeAgentTool(call));
        }
        return results;
    }

    private Map<String, Object> executeAgentTool(Map<String, Object> call) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("callId", call.get("callId"));
        result.put("kind", call.get("kind"));
        result.put("toolId", call.get("toolId"));
        result.put("code", call.get("code"));
        result.put("executedAt", LocalDateTime.now());
        String kind = AiRuntimeHelper.asString(call.get("kind"));
        String method = AiRuntimeHelper.firstText(AiRuntimeHelper.asString(call.get("method")), "GET").toUpperCase(Locale.ROOT);
        String endpoint = AiRuntimeHelper.firstText(AiRuntimeHelper.asString(call.get("endpoint")), "");
        if ("api-skill".equals(kind) && "GET".equals(method) && isLocalHealthEndpoint(endpoint)) {
            try {
                JsonNode body = RestClient.create()
                        .get()
                        .uri(localHealthUri())
                        .retrieve()
                        .body(JsonNode.class);
                result.put("status", "SUCCESS");
                result.put("mode", "local-readonly");
                result.put("output", body == null ? Map.of() : objectMapper.convertValue(body, new TypeReference<Map<String, Object>>() {}));
            } catch (RuntimeException ex) {
                result.put("status", "FAILED");
                result.put("mode", "local-readonly");
                result.put("error", ex.getMessage());
            }
            return result;
        }
        result.put("status", "SKIPPED");
        result.put("mode", "simulated");
        result.put("output", Map.of(
                "message", "Tool execution was simulated by the first safe runtime release.",
                "reason", "Only API Skill GET /actuator/health on this service is allowed to execute.",
                "endpoint", endpoint
        ));
        return result;
    }

    private boolean isLocalHealthEndpoint(String endpoint) {
        if (!StringUtils.hasText(endpoint)) return false;
        String trimmed = endpoint.trim();
        if ("/actuator/health".equals(trimmed)) return true;
        String lowerEndpoint = AiRuntimeHelper.lower(trimmed);
        return (lowerEndpoint.startsWith("http://localhost")
                || lowerEndpoint.startsWith("http://127.0.0.1")
                || lowerEndpoint.startsWith("http://[::1]"))
                && lowerEndpoint.endsWith("/actuator/health");
    }

    private String localHealthUri() {
        String port = AiRuntimeHelper.firstText(environment.getProperty("local.server.port"), environment.getProperty("server.port"), "8080");
        return "http://localhost:" + port + "/actuator/health";
    }

    private List<Map<String, Object>> appendToolResult(List<Map<String, Object>> existing, Map<String, Object> extra) {
        List<Map<String, Object>> results = new ArrayList<>(existing == null ? List.of() : existing);
        results.add(extra);
        return results;
    }

    private Map<String, Object> chatBiToolResult(ChatBiAskResponse chatBi) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("callId", "chatbi-" + UUID.randomUUID());
        result.put("kind", "chatbi");
        result.put("status", chatBi == null ? "SKIPPED" : "SUCCESS");
        result.put("mode", "draft-only");
        result.put("output", chatBi == null ? Map.of() : Map.of(
                "candidateSql", chatBi.getCandidateSql(),
                "executionPolicy", chatBi.getExecutionPolicy(),
                "executable", chatBi.getExecutable(),
                "warnings", chatBi.getSafetyWarnings()
        ));
        return result;
    }

    private List<Map<String, Object>> resolveAgentTools(String toolIds) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (String token : parseTokens(toolIds)) {
            if (token.startsWith("api:")) {
                Long id = AiRuntimeHelper.toLong(token);
                AiApiSkill apiSkill = safeSelectById(apiSkillMapper, id);
                if (apiSkill != null) tools.add(apiSkillMap(apiSkill));
                continue;
            }
            if (token.startsWith("mcp:")) {
                Long id = AiRuntimeHelper.toLong(token);
                AiMcpTool mcpTool = safeSelectById(mcpToolMapper, id);
                if (mcpTool != null) tools.add(mcpToolMap(mcpTool));
                continue;
            }
            Long id = AiRuntimeHelper.toLong(token);
            if (id != null) {
                AiApiSkill apiSkill = safeSelectById(apiSkillMapper, id);
                if (apiSkill != null) tools.add(apiSkillMap(apiSkill));
                AiMcpTool mcpTool = safeSelectById(mcpToolMapper, id);
                if (mcpTool != null) tools.add(mcpToolMap(mcpTool));
            } else {
                AiApiSkill apiSkill = safeSelectOne(apiSkillMapper, new QueryWrapper<AiApiSkill>().eq("skill_code", token).last("limit 1"));
                if (apiSkill != null) tools.add(apiSkillMap(apiSkill));
                AiMcpTool mcpTool = safeSelectOne(mcpToolMapper, new QueryWrapper<AiMcpTool>().eq("tool_code", token).last("limit 1"));
                if (mcpTool != null) tools.add(mcpToolMap(mcpTool));
            }
        }
        return tools;
    }

    private Map<String, Object> apiSkillMap(AiApiSkill skill) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", "api-skill");
        map.put("id", skill.getApiSkillId());
        map.put("code", skill.getSkillCode());
        map.put("name", skill.getSkillName());
        map.put("method", skill.getHttpMethod());
        map.put("endpoint", skill.getEndpoint());
        map.put("requestSchema", parseJson(skill.getRequestSchema()));
        map.put("responseSchema", parseJson(skill.getResponseSchema()));
        return map;
    }

    private Map<String, Object> mcpToolMap(AiMcpTool tool) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", "mcp-tool");
        map.put("id", tool.getMcpToolId());
        map.put("code", tool.getToolCode());
        map.put("name", tool.getToolName());
        map.put("serverName", tool.getServerName());
        map.put("transportType", tool.getTransportType());
        map.put("endpoint", tool.getEndpoint());
        map.put("inputSchema", parseJson(tool.getInputSchema()));
        map.put("outputSchema", parseJson(tool.getOutputSchema()));
        return map;
    }

    private String buildAgentPrompt(AiAgent agent, AiPromptTemplate prompt, List<Map<String, Object>> tools) {
        StringBuilder builder = new StringBuilder();
        builder.append(AiRuntimeHelper.firstText(prompt == null ? null : prompt.getSystemPrompt(), "You are an enterprise AI agent."));
        builder.append("\nAgent: ").append(agent.getAgentName());
        if (StringUtils.hasText(agent.getDescription())) {
            builder.append("\nDescription: ").append(agent.getDescription());
        }
        if (!tools.isEmpty()) {
            builder.append("\nAvailable tool metadata: ").append(toJsonString(tools));
        }
        builder.append("\nUse RAG citations when provided and explain if a real external action was not executed.");
        return builder.toString();
    }

    private String buildAgentRuntimePrompt(AiAgent agent, AiPromptTemplate prompt, List<Map<String, Object>> tools,
                                           List<Map<String, Object>> plan, RagSearchResponse rag,
                                           List<Map<String, Object>> toolResults, ChatBiAskResponse chatBi) {
        StringBuilder builder = new StringBuilder(buildAgentPrompt(agent, prompt, tools));
        builder.append("\n\nRuntime plan:\n").append(toJsonString(plan));
        String ragContext = ragSearchService.buildRagContext(rag);
        builder.append("\n\nRAG summary:\n");
        builder.append(StringUtils.hasText(ragContext) ? ragContext : "No retrieved knowledge chunks were available for this run.");
        builder.append("\n\nTool results:\n").append(toJsonString(toolResults));
        builder.append("\n\nChatBI draft:\n");
        if (chatBi == null) {
            builder.append("Not requested.");
        } else {
            builder.append(toJsonString(Map.of(
                    "candidateSql", chatBi.getCandidateSql(),
                    "executionPolicy", chatBi.getExecutionPolicy(),
                    "executable", chatBi.getExecutable(),
                    "warnings", chatBi.getSafetyWarnings(),
                    "explanation", chatBi.getExplanation()
            )));
        }
        builder.append("\n\nAnswer requirements: cite retrieved chunks when useful, mention skipped/simulated tools clearly, and never claim SQL or external tools were executed unless tool results say SUCCESS.");
        return builder.toString();
    }

    private Map<String, Object> agentRagMetadata(RagSearchResponse rag, boolean enabled, List<Long> knowledgeBaseIds) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", enabled);
        metadata.put("knowledgeBaseIds", knowledgeBaseIds);
        metadata.put("hitCount", rag == null ? 0 : rag.getTotal());
        metadata.put("citations", rag == null ? List.of() : rag.getHits().stream().map(RagSearchHit::getCitation).toList());
        metadata.put("hits", rag == null ? List.of() : rag.getHits());
        metadata.put("search", rag == null ? Map.of() : rag.getMetadata());
        return metadata;
    }

    private Map<String, Object> agentChatBiMetadata(ChatBiAskResponse chatBi, boolean enabled) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", enabled);
        metadata.put("candidateSql", chatBi == null ? null : chatBi.getCandidateSql());
        metadata.put("executionPolicy", chatBi == null ? null : chatBi.getExecutionPolicy());
        metadata.put("executable", chatBi == null ? null : chatBi.getExecutable());
        metadata.put("warnings", chatBi == null ? List.of() : chatBi.getSafetyWarnings());
        metadata.put("matchedTerms", chatBi == null ? List.of() : chatBi.getMatchedTerms());
        metadata.put("tables", chatBi == null ? List.of() : chatBi.getTables());
        return metadata;
    }

    private boolean isRagEnabled(Map<String, Object> options) {
        Object rag = options.get("rag");
        Object useRag = options.get("useRag");
        return !Boolean.FALSE.equals(AiRuntimeHelper.toBoolean(rag)) && !Boolean.FALSE.equals(AiRuntimeHelper.toBoolean(useRag));
    }

    private List<String> parseTokens(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("[,，\\s]+")) {
            if (StringUtils.hasText(token)) tokens.add(token.trim());
        }
        return tokens;
    }

    private List<Long> parseLongList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("[,，\\s]+")) {
            if (StringUtils.hasText(token)) tokens.add(token.trim());
        }
        return tokens.stream().map(AiRuntimeHelper::toLong).filter(java.util.Objects::nonNull).toList();
    }

    private Map<String, Object> parseJson(String text) {
        if (!StringUtils.hasText(text)) return Map.of();
        try {
            JsonNode node = objectMapper.readTree(text);
            if (node.isObject()) return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", objectMapper.convertValue(node, Object.class));
            return map;
        } catch (Exception ex) {
            return Map.of("raw", text);
        }
    }

    private String toJsonString(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { return String.valueOf(value); }
    }

    private Map<String, Object> safeOptions(Map<String, Object> options) {
        return options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    private <T> T safeSelectById(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, java.io.Serializable id) {
        try { return mapper.selectById(id); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return null; throw ex; }
    }

    private <T> T safeSelectOne(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, QueryWrapper<T> wrapper) {
        try { return mapper.selectOne(wrapper); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return null; throw ex; }
    }
}