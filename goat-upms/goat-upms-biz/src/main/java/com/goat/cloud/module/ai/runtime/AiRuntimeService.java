package com.goat.cloud.module.ai.runtime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.module.ai.entity.AiAgent;
import com.goat.cloud.module.ai.entity.AiApiSkill;
import com.goat.cloud.module.ai.entity.AiBillingRecord;
import com.goat.cloud.module.ai.entity.AiChatBiDatasource;
import com.goat.cloud.module.ai.entity.AiChatBiDataset;
import com.goat.cloud.module.ai.entity.AiChatBiTable;
import com.goat.cloud.module.ai.entity.AiChatBiTerm;
import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.entity.AiDocumentChunk;
import com.goat.cloud.module.ai.entity.AiKnowledgeBase;
import com.goat.cloud.module.ai.entity.AiMcpTool;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.entity.AiPromptTemplate;
import com.goat.cloud.module.ai.entity.AiWorkflow;
import com.goat.cloud.module.ai.mapper.AiAgentMapper;
import com.goat.cloud.module.ai.mapper.AiApiSkillMapper;
import com.goat.cloud.module.ai.mapper.AiBillingRecordMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasetMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTableMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTermMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentMapper;
import com.goat.cloud.module.ai.mapper.AiKnowledgeBaseMapper;
import com.goat.cloud.module.ai.mapper.AiMcpToolMapper;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.mapper.AiPromptTemplateMapper;
import com.goat.cloud.module.ai.mapper.AiWorkflowMapper;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatMessage;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.model.vo.AiTokenUsageVO;
import com.goat.cloud.module.ai.runtime.model.AgentRunRequest;
import com.goat.cloud.module.ai.runtime.model.AgentRunResponse;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse;
import com.goat.cloud.module.ai.runtime.model.ChatBiTableSnapshot;
import com.goat.cloud.module.ai.runtime.model.ChatBiTermMatch;
import com.goat.cloud.module.ai.runtime.model.RagSearchHit;
import com.goat.cloud.module.ai.runtime.model.RagSearchRequest;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import com.goat.cloud.module.ai.runtime.model.WorkflowNodeTrace;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunRequest;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class AiRuntimeService {

    private static final int DEFAULT_RAG_TOP_K = 5;
    private static final int MAX_RAG_TOP_K = 20;
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s,，。.!！?？;；:：()（）\\[\\]{}<>《》\"'`]+");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern MUTATION_SQL = Pattern.compile("(?i)\\b(insert|update|delete|drop|truncate|alter|grant|revoke|merge|call|execute)\\b");

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final AiModelConfigMapper modelConfigMapper;
    private final AiPromptTemplateMapper promptTemplateMapper;
    private final AiBillingRecordMapper billingRecordMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiDocumentMapper documentMapper;
    private final AiDocumentChunkMapper documentChunkMapper;
    private final AiApiSkillMapper apiSkillMapper;
    private final AiMcpToolMapper mcpToolMapper;
    private final AiChatBiDatasourceMapper chatBiDatasourceMapper;
    private final AiChatBiTableMapper chatBiTableMapper;
    private final AiChatBiDatasetMapper chatBiDatasetMapper;
    private final AiChatBiTermMapper chatBiTermMapper;
    private final AiAgentMapper agentMapper;
    private final AiWorkflowMapper workflowMapper;

    public RagSearchResponse search(RagSearchRequest request) {
        RagSearchRequest safeRequest = request == null ? new RagSearchRequest() : request;
        String query = normalizeText(safeRequest.getQuery());
        int topK = clamp(safeRequest.getTopK(), DEFAULT_RAG_TOP_K, 1, MAX_RAG_TOP_K);
        List<String> terms = searchTerms(query);

        List<AiDocumentChunk> chunks = safeSelectList(documentChunkMapper, buildChunkQuery(safeRequest, terms, topK));
        Map<Long, AiDocument> documents = selectByIds(documentMapper, chunks.stream()
                .map(AiDocumentChunk::getDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, AiKnowledgeBase> knowledgeBases = selectByIds(knowledgeBaseMapper, chunks.stream()
                .map(AiDocumentChunk::getKnowledgeBaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        boolean includeContent = Boolean.TRUE.equals(safeRequest.getIncludeContent());
        List<RagSearchHit> hits = chunks.stream()
                .map(chunk -> toHit(chunk, documents.get(chunk.getDocumentId()), knowledgeBases.get(chunk.getKnowledgeBaseId()), query, terms, includeContent))
                .filter(hit -> !StringUtils.hasText(query) || hit.getScore() > 0)
                .sorted(Comparator.comparing(RagSearchHit::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RagSearchHit::getChunkId, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(topK)
                .toList();

        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setTotal(hits.size());
        response.setHits(hits);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topK", topK);
        metadata.put("searchMode", "postgres-keyword-plus-local-scoring");
        metadata.put("candidateCount", chunks.size());
        metadata.put("knowledgeBaseIds", effectiveKnowledgeBaseIds(safeRequest));
        metadata.put("documentId", safeRequest.getDocumentId());
        response.setMetadata(metadata);
        response.setSearchedAt(LocalDateTime.now());
        return response;
    }

    public AiChatResponse chat(AiChatRequest request) {
        AiChatRequest safeRequest = request == null ? new AiChatRequest() : request;
        Map<String, Object> options = safeOptions(safeRequest.getOptions());
        String conversationId = StringUtils.hasText(safeRequest.getConversationId())
                ? safeRequest.getConversationId()
                : UUID.randomUUID().toString();
        String userText = StringUtils.hasText(safeRequest.getMessage())
                ? safeRequest.getMessage()
                : latestUserText(safeRequest.getMessages());

        AiModelConfig model = resolveModel(safeRequest);
        String provider = firstText(safeRequest.getProvider(), model == null ? null : model.getProvider(), "local-runtime");
        String modelCode = firstText(safeRequest.getModelCode(), model == null ? null : model.getModelCode(), "local-rules");
        String systemPrompt = firstText(safeRequest.getSystemPrompt(), "You are Techen Cloud enterprise AI assistant. Answer with concise, grounded reasoning.");

        RagSearchResponse rag = null;
        if (StringUtils.hasText(userText) && isRagEnabled(options)) {
            RagSearchRequest ragRequest = new RagSearchRequest();
            ragRequest.setQuery(userText);
            ragRequest.setTopK(toInteger(options.get("topK"), DEFAULT_RAG_TOP_K));
            ragRequest.setIncludeContent(Boolean.TRUE);
            ragRequest.setKnowledgeBaseId(toLong(options.get("knowledgeBaseId")));
            ragRequest.setKnowledgeBaseIds(toLongList(options.get("knowledgeBaseIds")));
            ragRequest.setDocumentId(toLong(options.get("documentId")));
            rag = search(ragRequest);
        }

        String ragContext = buildRagContext(rag);
        String apiKey = resolveApiKey(model == null ? null : model.getApiKeyRef());
        boolean canCallProvider = model != null
                && StringUtils.hasText(model.getEndpoint())
                && StringUtils.hasText(apiKey);
        ProviderResult providerResult = canCallProvider
                ? callOpenAiCompatible(model, apiKey, systemPrompt, ragContext, userText, safeRequest)
                : ProviderResult.skipped(missingProviderReason(model, apiKey));

        String answer = providerResult.success()
                ? providerResult.content()
                : localAnswerClean(userText, rag, providerResult.error());
        AiTokenUsageVO usage = providerResult.usage() == null
                ? estimateUsage(systemPrompt + "\n" + ragContext + "\n" + userText, answer)
                : providerResult.usage();

        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversationId);
        response.setProvider(provider);
        response.setModelCode(modelCode);
        response.setMessage(new AiChatMessage("assistant", answer));
        response.setFinishReason(providerResult.success() ? firstText(providerResult.finishReason(), "stop") : "local_fallback");
        response.setMock(!providerResult.success());
        response.setUsage(usage);
        response.setMetadata(chatMetadata(model, rag, options, providerResult));
        response.setCreatedAt(LocalDateTime.now());

        recordBilling(conversationId, provider, modelCode, firstText(asString(options.get("bizType")), "CHAT"), usage, response.getFinishReason());
        return response;
    }

    public ChatBiAskResponse askChatBi(ChatBiAskRequest request) {
        ChatBiAskRequest safeRequest = request == null ? new ChatBiAskRequest() : request;
        String question = normalizeText(safeRequest.getQuestion());
        AiChatBiDataset dataset = resolveDataset(safeRequest);
        Long datasourceId = firstLong(safeRequest.getDatasourceId(), dataset == null ? null : dataset.getDatasourceId());
        AiChatBiDatasource datasource = datasourceId == null ? null : safeSelectById(chatBiDatasourceMapper, datasourceId);
        List<AiChatBiTable> tables = resolveTables(dataset, datasourceId);
        List<AiChatBiTerm> terms = resolveTerms(dataset);
        List<ChatBiTermMatch> matchedTerms = terms.stream()
                .map(term -> toTermMatch(term, question))
                .filter(match -> match.getScore() > 0)
                .sorted(Comparator.comparing(ChatBiTermMatch::getScore, Comparator.reverseOrder()))
                .limit(5)
                .toList();

        List<String> warnings = new ArrayList<>();
        warnings.add("Candidate SQL is draft-only and was not executed.");
        if (StringUtils.hasText(question) && MUTATION_SQL.matcher(question).find()) {
            warnings.add("The question contains mutation keywords; only read-only SELECT SQL was generated.");
        }
        String candidateSql = buildCandidateSql(question, dataset, tables, matchedTerms, safeRequest, warnings);

        ChatBiAskResponse response = new ChatBiAskResponse();
        response.setQuestion(question);
        response.setDatasetId(dataset == null ? null : dataset.getDatasetId());
        response.setDatasetCode(dataset == null ? null : dataset.getDatasetCode());
        response.setDatasetName(dataset == null ? null : dataset.getDatasetName());
        response.setDatasourceId(datasource == null ? datasourceId : datasource.getDatasourceId());
        response.setDatasourceName(datasource == null ? null : datasource.getDatasourceName());
        response.setCandidateSql(candidateSql);
        response.setExecutable(Boolean.FALSE);
        response.setExecutionPolicy("DRAFT_ONLY_NOT_EXECUTED");
        response.setSafetyWarnings(warnings);
        response.setMatchedTerms(matchedTerms);
        response.setTables(tables.stream().map(this::toTableSnapshot).toList());
        response.setExplanation(buildChatBiExplanation(dataset, datasource, tables, matchedTerms));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("semanticModel", parseJson(dataset == null ? null : dataset.getSemanticModel()));
        metadata.put("defaultFilters", dataset == null ? null : dataset.getDefaultFilters());
        metadata.put("generationMode", "metadata-driven-safe-select");
        response.setMetadata(metadata);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

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
        boolean toolingEnabled = !tools.isEmpty() && !Boolean.FALSE.equals(toBoolean(options.get("useTools")));
        boolean chatBiEnabled = shouldUseChatBi(safeRequest.getMessage(), options);
        List<Map<String, Object>> plan = buildAgentRunPlan(agent, safeRequest, knowledgeBaseIds, tools, ragEnabled, toolingEnabled, chatBiEnabled);

        RagSearchResponse rag = null;
        if (ragEnabled && StringUtils.hasText(safeRequest.getMessage())) {
            RagSearchRequest ragRequest = new RagSearchRequest();
            ragRequest.setQuery(safeRequest.getMessage());
            ragRequest.setTopK(toInteger(options.get("topK"), DEFAULT_RAG_TOP_K));
            ragRequest.setIncludeContent(Boolean.TRUE);
            ragRequest.setKnowledgeBaseIds(knowledgeBaseIds);
            rag = search(ragRequest);
        }

        List<Map<String, Object>> toolCalls = toolingEnabled ? buildToolCalls(tools, safeRequest) : List.of();
        List<Map<String, Object>> toolResults = executeAgentTools(toolCalls);

        ChatBiAskResponse chatBi = null;
        if (chatBiEnabled) {
            ChatBiAskRequest chatBiRequest = new ChatBiAskRequest();
            chatBiRequest.setQuestion(safeRequest.getMessage());
            chatBiRequest.setDatasetId(toLong(options.get("chatBiDatasetId")));
            chatBiRequest.setDatasetCode(asString(options.get("chatBiDatasetCode")));
            chatBiRequest.setDatasourceId(toLong(options.get("chatBiDatasourceId")));
            chatBiRequest.setLimit(toInteger(options.get("chatBiLimit"), 100));
            chatBiRequest.setOptions(options);
            chatBi = askChatBi(chatBiRequest);
            toolResults = appendToolResult(toolResults, chatBiToolResult(chatBi));
        }

        AiChatRequest chatRequest = new AiChatRequest();
        chatRequest.setConversationId(safeRequest.getConversationId());
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
        AiChatResponse chat = chat(chatRequest);

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

    public WorkflowRunResponse runWorkflow(Long workflowId, WorkflowRunRequest request) {
        if (workflowId == null) {
            throw new BusinessException(4001, "Workflow id is required");
        }
        AiWorkflow workflow = safeSelectById(workflowMapper, workflowId);
        if (workflow == null) {
            throw new BusinessException(4045, "Workflow not found");
        }
        WorkflowRunRequest safeRequest = request == null ? new WorkflowRunRequest() : request;
        LocalDateTime startedAt = LocalDateTime.now();
        String runId = UUID.randomUUID().toString();
        JsonNode graph = parseGraph(workflow.getGraphJson());
        List<JsonNode> nodes = graphNodes(graph);
        if (nodes.isEmpty()) {
            nodes = defaultWorkflowNodes();
        }

        List<WorkflowNodeTrace> traces = new ArrayList<>();
        Object lastOutput = safeRequest.getInput() == null ? Map.of("message", safeRequest.getMessage()) : safeRequest.getInput();
        for (JsonNode node : nodes) {
            LocalDateTime nodeStart = LocalDateTime.now();
            WorkflowNodeTrace trace = new WorkflowNodeTrace();
            trace.setNodeId(nodeText(node, "id", UUID.randomUUID().toString()));
            trace.setNodeType(nodeText(node, "type", "task"));
            trace.setNodeName(nodeName(node));
            trace.setInput(lastOutput);
            try {
                lastOutput = executeWorkflowNode(node, workflow, safeRequest, lastOutput);
                trace.setOutput(lastOutput);
                trace.setStatus("SUCCESS");
            } catch (RuntimeException ex) {
                trace.setOutput(Map.of("error", ex.getMessage()));
                trace.setStatus("FAILED");
                lastOutput = trace.getOutput();
            }
            trace.setDurationMs(Duration.between(nodeStart, LocalDateTime.now()).toMillis());
            traces.add(trace);
        }

        WorkflowRunResponse response = new WorkflowRunResponse();
        response.setRunId(runId);
        response.setWorkflowId(workflow.getWorkflowId());
        response.setWorkflowCode(workflow.getWorkflowCode());
        response.setWorkflowName(workflow.getWorkflowName());
        response.setVersion(workflow.getVersion());
        response.setStatus(traces.stream().anyMatch(trace -> "FAILED".equals(trace.getStatus())) ? "PARTIAL_FAILED" : "SUCCESS");
        response.setSummary(workflowSummary(workflow, traces, lastOutput));
        response.setOutput(lastOutput);
        response.setTraces(traces);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("triggerType", workflow.getTriggerType());
        metadata.put("edgeCount", graph.path("edges").isArray() ? graph.path("edges").size() : 0);
        metadata.put("nodeCount", nodes.size());
        metadata.put("runtime", "graph-json-sequential-runtime");
        response.setMetadata(metadata);
        response.setStartedAt(startedAt);
        response.setCompletedAt(LocalDateTime.now());
        return response;
    }

    private QueryWrapper<AiDocumentChunk> buildChunkQuery(RagSearchRequest request, List<String> terms, int topK) {
        QueryWrapper<AiDocumentChunk> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ENABLED");
        if (request.getDocumentId() != null) {
            wrapper.eq("document_id", request.getDocumentId());
        }
        List<Long> knowledgeBaseIds = effectiveKnowledgeBaseIds(request);
        if (!knowledgeBaseIds.isEmpty()) {
            wrapper.in("knowledge_base_id", knowledgeBaseIds);
        }
        if (StringUtils.hasText(request.getQuery())) {
            wrapper.and(nested -> {
                nested.like("title", request.getQuery()).or().like("content", request.getQuery());
                for (String term : terms) {
                    nested.or().like("title", term).or().like("content", term);
                }
            });
        }
        return wrapper.orderByDesc("create_time").last("limit " + Math.max(50, topK * 20));
    }

    private RagSearchHit toHit(AiDocumentChunk chunk, AiDocument document, AiKnowledgeBase knowledgeBase,
                               String query, List<String> terms, boolean includeContent) {
        RagSearchHit hit = new RagSearchHit();
        hit.setChunkId(chunk.getChunkId());
        hit.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        hit.setKnowledgeBaseName(knowledgeBase == null ? null : knowledgeBase.getKnowledgeBaseName());
        hit.setDocumentId(chunk.getDocumentId());
        hit.setDocumentName(document == null ? null : document.getDocumentName());
        hit.setDocumentType(document == null ? null : document.getDocumentType());
        hit.setSourceUri(document == null ? null : document.getSourceUri());
        hit.setChunkIndex(chunk.getChunkIndex());
        hit.setTitle(chunk.getTitle());
        hit.setContent(includeContent ? chunk.getContent() : null);
        hit.setContentPreview(preview(chunk.getContent(), 240));
        hit.setTokenCount(chunk.getTokenCount());
        hit.setScore(scoreChunk(chunk, query, terms));
        hit.setCitation(citation(knowledgeBase, document, chunk));
        hit.setMetadata(parseJson(chunk.getMetadata()));
        return hit;
    }

    private double scoreChunk(AiDocumentChunk chunk, String query, List<String> terms) {
        if (!StringUtils.hasText(query)) {
            return 0.1D;
        }
        String title = lower(chunk.getTitle());
        String content = lower(chunk.getContent());
        String normalizedQuery = lower(query);
        double score = 0D;
        if (title.contains(normalizedQuery)) {
            score += 8D;
        }
        if (content.contains(normalizedQuery)) {
            score += 6D;
        }
        for (String term : terms) {
            String normalizedTerm = lower(term);
            if (normalizedTerm.length() < 2) {
                continue;
            }
            if (title.contains(normalizedTerm)) {
                score += 3D;
            }
            score += countOccurrences(content, normalizedTerm);
        }
        if (StringUtils.hasText(chunk.getTitle())) {
            score += 0.2D;
        }
        return Math.round(score * 100D) / 100D;
    }

    private ProviderResult callOpenAiCompatible(AiModelConfig model, String apiKey, String systemPrompt,
                                                String ragContext, String userText, AiChatRequest request) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            if (StringUtils.hasText(ragContext)) {
                messages.add(Map.of("role", "system", "content", "Use these retrieved citations when relevant:\n" + ragContext));
            }
            if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                for (AiChatMessage message : request.getMessages()) {
                    if (message != null && StringUtils.hasText(message.getRole()) && StringUtils.hasText(message.getContent())) {
                        messages.add(Map.of("role", message.getRole(), "content", message.getContent()));
                    }
                }
            } else {
                messages.add(Map.of("role", "user", "content", firstText(userText, "Please summarize available context.")));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model.getModelCode());
            body.put("messages", messages);
            body.put("stream", Boolean.FALSE);
            if (request.getTemperature() != null) {
                body.put("temperature", request.getTemperature());
            }

            JsonNode json = RestClient.create()
                    .post()
                    .uri(completionUrl(model.getEndpoint()))
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String content = json == null ? null : json.path("choices").path(0).path("message").path("content").asText(null);
            if (!StringUtils.hasText(content)) {
                return ProviderResult.failed("Provider returned no assistant content");
            }
            AiTokenUsageVO usage = null;
            if (json.has("usage")) {
                JsonNode usageNode = json.path("usage");
                int promptTokens = usageNode.path("prompt_tokens").asInt(0);
                int completionTokens = usageNode.path("completion_tokens").asInt(0);
                usage = new AiTokenUsageVO(promptTokens, completionTokens, usageNode.path("total_tokens").asInt(promptTokens + completionTokens));
            }
            String finishReason = json.path("choices").path(0).path("finish_reason").asText("stop");
            return ProviderResult.ok(content, finishReason, usage);
        } catch (RuntimeException ex) {
            return ProviderResult.failed(ex.getMessage());
        }
    }

    private String localAnswer(String userText, RagSearchResponse rag, String fallbackReason) {
        StringBuilder answer = new StringBuilder();
        answer.append("本地规则回答：当前未完成外部大模型调用，系统已基于 Techen Cloud AI 元数据和 RAG 检索生成可解释结果。");
        if (StringUtils.hasText(fallbackReason)) {
            answer.append("降级原因：").append(fallbackReason).append("。");
        }
        if (rag != null && rag.getHits() != null && !rag.getHits().isEmpty()) {
            answer.append("针对问题“").append(firstText(userText, "未提供问题")).append("”，检索到 ")
                    .append(rag.getHits().size()).append(" 个相关切片：");
            for (int i = 0; i < rag.getHits().size(); i++) {
                RagSearchHit hit = rag.getHits().get(i);
                answer.append(i + 1).append(". [").append(hit.getCitation()).append("] ")
                        .append(firstText(hit.getContentPreview(), hit.getTitle(), "无预览")).append(" ");
            }
            answer.append("生产环境接入向量化和真实模型后，可将这些引用作为上下文生成更完整答案。");
        } else {
            answer.append("未检索到可引用的知识库切片，请先导入文档、完成切片和向量化，或在请求中指定 knowledgeBaseId。");
        }
        return answer.toString();
    }

    private Map<String, Object> chatMetadata(AiModelConfig model, RagSearchResponse rag, Map<String, Object> options, ProviderResult providerResult) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("modelId", model == null ? null : model.getModelId());
        metadata.put("modelName", model == null ? null : model.getModelName());
        metadata.put("endpointConfigured", model != null && StringUtils.hasText(model.getEndpoint()));
        metadata.put("apiKeyRefConfigured", model != null && StringUtils.hasText(model.getApiKeyRef()));
        metadata.put("providerCall", providerResult.success() ? "SUCCESS" : "LOCAL_FALLBACK");
        metadata.put("fallbackReason", providerResult.success() ? null : providerResult.error());
        metadata.put("options", options);
        Map<String, Object> ragMetadata = new LinkedHashMap<>();
        ragMetadata.put("enabled", rag != null);
        ragMetadata.put("hitCount", rag == null ? 0 : rag.getTotal());
        ragMetadata.put("citations", rag == null ? List.of() : rag.getHits().stream().map(RagSearchHit::getCitation).toList());
        ragMetadata.put("hits", rag == null ? List.of() : rag.getHits());
        metadata.put("rag", ragMetadata);
        return metadata;
    }

    private String localAnswerClean(String userText, RagSearchResponse rag, String fallbackReason) {
        StringBuilder answer = new StringBuilder();
        answer.append("本地规则回答：当前未完成外部大模型调用，系统已基于 Techen Cloud AI 元数据和 RAG 检索生成可解释结果。");
        if (StringUtils.hasText(fallbackReason)) {
            answer.append("降级原因：").append(fallbackReason).append("。");
        }
        if (rag != null && rag.getHits() != null && !rag.getHits().isEmpty()) {
            answer.append("针对问题“").append(firstText(userText, "未提供问题")).append("”，检索到 ")
                    .append(rag.getHits().size()).append(" 个相关切片：");
            for (int i = 0; i < rag.getHits().size(); i++) {
                RagSearchHit hit = rag.getHits().get(i);
                answer.append(i + 1).append(". [").append(hit.getCitation()).append("] ")
                        .append(firstText(hit.getContentPreview(), hit.getTitle(), "无预览")).append(" ");
            }
            answer.append("生产环境接入向量化和真实模型后，可将这些引用作为上下文生成更完整答案。");
        } else {
            answer.append("未检索到可引用的知识库切片，请先导入文档、完成切片和向量化，或在请求中指定 knowledgeBaseId。");
        }
        return answer.toString();
    }

    private String buildCandidateSql(String question, AiChatBiDataset dataset, List<AiChatBiTable> tables,
                                     List<ChatBiTermMatch> matchedTerms, ChatBiAskRequest request, List<String> warnings) {
        if (tables.isEmpty()) {
            warnings.add("No table metadata is available for the selected dataset/datasource.");
            return null;
        }
        AiChatBiTable table = tables.get(0);
        String tableRef = tableReference(table);
        if (!StringUtils.hasText(tableRef)) {
            warnings.add("Table identifier is unsafe or incomplete, SQL was not generated.");
            return null;
        }
        int limit = clamp(request.getLimit(), 100, 1, 500);
        String lowerQuestion = lower(question);
        boolean detailMode = lowerQuestion.contains("list") || lowerQuestion.contains("show")
                || question.contains("明细") || question.contains("列表") || question.contains("哪些");
        String filter = safeFilter(dataset == null ? null : dataset.getDefaultFilters(), warnings);
        String whereClause = StringUtils.hasText(filter) ? " where " + filter : "";
        if (detailMode) {
            return "select " + selectColumns(table) + " from " + tableRef + whereClause + " limit " + limit;
        }

        String expression = matchedTerms.isEmpty() ? "count(*)" : firstText(matchedTerms.get(0).getExpression(), "count(*)");
        if (!isSafeReadExpression(expression)) {
            warnings.add("Matched term expression is not safe enough for a draft SQL, falling back to count(*).");
            expression = "count(*)";
        }
        String alias = matchedTerms.isEmpty() ? "metric_value" : safeAlias(firstText(matchedTerms.get(0).getTermCode(), matchedTerms.get(0).getTermName(), "metric_value"));
        return "select " + expression + " as " + alias + " from " + tableRef + whereClause + " limit 1";
    }

    private ChatBiTermMatch toTermMatch(AiChatBiTerm term, String question) {
        ChatBiTermMatch match = new ChatBiTermMatch();
        match.setTermId(term.getTermId());
        match.setTermCode(term.getTermCode());
        match.setTermName(term.getTermName());
        match.setExpression(term.getExpression());
        match.setDefinition(term.getDefinition());
        double score = 0D;
        String haystack = lower(String.join(" ",
                firstText(term.getTermCode(), ""),
                firstText(term.getTermName(), ""),
                firstText(term.getSynonyms(), ""),
                firstText(term.getDefinition(), "")));
        for (String termText : searchTerms(question)) {
            if (haystack.contains(lower(termText))) {
                score += 2D;
            }
        }
        if (StringUtils.hasText(term.getExpression())) {
            score += 0.5D;
        }
        match.setScore(Math.round(score * 100D) / 100D);
        match.setReason(score > 0 ? "Matched question text with term name/synonyms/definition." : "No keyword match.");
        return match;
    }

    private Object executeWorkflowNode(JsonNode node, AiWorkflow workflow, WorkflowRunRequest request, Object previousOutput) {
        String type = lower(nodeText(node, "type", "task"));
        String message = firstText(request.getMessage(), asString(request.getInput() == null ? null : request.getInput().get("message")), workflow.getDescription());
        if ("start".equals(type) || "input".equals(type)) {
            return Map.of("message", firstText(message, ""), "variables", request.getVariables() == null ? Map.of() : request.getVariables());
        }
        if ("rag".equals(type) || "retriever".equals(type) || "knowledge".equals(type) || "knowledge-search".equals(type)) {
            RagSearchRequest ragRequest = new RagSearchRequest();
            ragRequest.setQuery(message);
            ragRequest.setIncludeContent(Boolean.TRUE);
            ragRequest.setTopK(3);
            return search(ragRequest);
        }
        if ("llm".equals(type) || "chat".equals(type) || "model".equals(type) || "summary".equals(type)) {
            AiChatRequest chatRequest = new AiChatRequest();
            chatRequest.setConversationId(request.getConversationId());
            chatRequest.setMessage(message);
            chatRequest.setSystemPrompt("You are executing workflow " + workflow.getWorkflowName() + ". Summarize prior node output and answer safely.");
            Map<String, Object> options = safeOptions(request.getOptions());
            options.put("bizType", "WORKFLOW");
            options.put("workflowId", workflow.getWorkflowId());
            chatRequest.setOptions(options);
            return chat(chatRequest);
        }
        if ("agent".equals(type)) {
            Long agentId = nodeLong(node, "agentId");
            if (agentId == null) {
                return Map.of("mode", "simulated-agent", "message", "No agentId configured on workflow node.");
            }
            AgentRunRequest agentRequest = new AgentRunRequest();
            agentRequest.setConversationId(request.getConversationId());
            agentRequest.setMessage(message);
            agentRequest.setOptions(request.getOptions());
            return runAgent(agentId, agentRequest);
        }
        if ("chatbi".equals(type) || "ask".equals(type)) {
            ChatBiAskRequest askRequest = new ChatBiAskRequest();
            askRequest.setQuestion(message);
            return askChatBi(askRequest);
        }
        if ("api-skill".equals(type) || "tool".equals(type) || "mcp".equals(type)) {
            return simulatedToolOutput(node, previousOutput);
        }
        if ("condition".equals(type) || "review".equals(type) || "manual".equals(type)) {
            return Map.of("decision", "CONTINUE", "reason", "Runtime records the node but does not block on manual review yet.");
        }
        return Map.of("nodeType", type, "status", "RECORDED", "previousOutput", previousOutput);
    }

    private Object simulatedToolOutput(JsonNode node, Object previousOutput) {
        Long skillId = nodeLong(node, "skillId");
        Long mcpToolId = nodeLong(node, "mcpToolId");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("mode", "metadata-only");
        output.put("executed", Boolean.FALSE);
        output.put("previousOutput", previousOutput);
        if (skillId != null) {
            AiApiSkill skill = safeSelectById(apiSkillMapper, skillId);
            output.put("apiSkill", skill == null ? null : apiSkillMap(skill));
        }
        if (mcpToolId != null) {
            AiMcpTool tool = safeSelectById(mcpToolMapper, mcpToolId);
            output.put("mcpTool", tool == null ? null : mcpToolMap(tool));
        }
        if (skillId == null && mcpToolId == null) {
            output.put("note", "Tool node has no skillId/mcpToolId; no external call was made.");
        }
        return output;
    }

    private List<Map<String, Object>> buildAgentRunPlan(AiAgent agent, AgentRunRequest request, List<Long> knowledgeBaseIds,
                                                        List<Map<String, Object>> tools, boolean ragEnabled,
                                                        boolean toolingEnabled, boolean chatBiEnabled) {
        List<Map<String, Object>> plan = new ArrayList<>();
        plan.add(planStep("understand", true, "Analyze the user message, agent role, variables, and memory policy.",
                Map.of("messagePreview", preview(request.getMessage(), 160), "hasVariables", request.getVariables() != null && !request.getVariables().isEmpty())));
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
        if (Boolean.TRUE.equals(toBoolean(options.get("useChatBi")))) {
            return true;
        }
        String text = lower(firstText(message, ""));
        return text.contains("count")
                || text.contains("统计")
                || text.contains("多少")
                || text.contains("查询");
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
                    "message", firstText(request.getMessage(), ""),
                    "variables", request.getVariables() == null ? Map.of() : request.getVariables()
            ));
            calls.add(call);
        }
        return calls;
    }

    private List<Map<String, Object>> executeAgentTools(List<Map<String, Object>> toolCalls) {
        if (toolCalls.isEmpty()) {
            return List.of();
        }
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
        String kind = asString(call.get("kind"));
        String method = firstText(asString(call.get("method")), "GET").toUpperCase(Locale.ROOT);
        String endpoint = firstText(asString(call.get("endpoint")), "");
        if ("api-skill".equals(kind) && "GET".equals(method) && isLocalHealthEndpoint(endpoint)) {
            try {
                JsonNode body = RestClient.create()
                        .get()
                        .uri(localHealthUri())
                        .retrieve()
                        .body(JsonNode.class);
                result.put("status", "SUCCESS");
                result.put("mode", "local-readonly");
                result.put("output", body == null ? Map.of() : objectMapper.convertValue(body, new TypeReference<Map<String, Object>>() {
                }));
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
        if (!StringUtils.hasText(endpoint)) {
            return false;
        }
        String trimmed = endpoint.trim();
        if ("/actuator/health".equals(trimmed)) {
            return true;
        }
        String lowerEndpoint = lower(trimmed);
        return (lowerEndpoint.startsWith("http://localhost")
                || lowerEndpoint.startsWith("http://127.0.0.1")
                || lowerEndpoint.startsWith("http://[::1]"))
                && lowerEndpoint.endsWith("/actuator/health");
    }

    private String localHealthUri() {
        String port = firstText(environment.getProperty("local.server.port"), environment.getProperty("server.port"), "8080");
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
                Long id = toLong(token);
                AiApiSkill apiSkill = safeSelectById(apiSkillMapper, id);
                if (apiSkill != null) {
                    tools.add(apiSkillMap(apiSkill));
                }
                continue;
            }
            if (token.startsWith("mcp:")) {
                Long id = toLong(token);
                AiMcpTool mcpTool = safeSelectById(mcpToolMapper, id);
                if (mcpTool != null) {
                    tools.add(mcpToolMap(mcpTool));
                }
                continue;
            }
            Long id = toLong(token);
            if (id != null) {
                AiApiSkill apiSkill = safeSelectById(apiSkillMapper, id);
                if (apiSkill != null) {
                    tools.add(apiSkillMap(apiSkill));
                }
                AiMcpTool mcpTool = safeSelectById(mcpToolMapper, id);
                if (mcpTool != null) {
                    tools.add(mcpToolMap(mcpTool));
                }
            } else {
                AiApiSkill apiSkill = safeSelectOne(apiSkillMapper, new QueryWrapper<AiApiSkill>().eq("skill_code", token).last("limit 1"));
                if (apiSkill != null) {
                    tools.add(apiSkillMap(apiSkill));
                }
                AiMcpTool mcpTool = safeSelectOne(mcpToolMapper, new QueryWrapper<AiMcpTool>().eq("tool_code", token).last("limit 1"));
                if (mcpTool != null) {
                    tools.add(mcpToolMap(mcpTool));
                }
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
        builder.append(firstText(prompt == null ? null : prompt.getSystemPrompt(), "You are an enterprise AI agent."));
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
        String ragContext = buildRagContext(rag);
        builder.append("\n\nRAG summary:\n");
        if (StringUtils.hasText(ragContext)) {
            builder.append(ragContext);
        } else {
            builder.append("No retrieved knowledge chunks were available for this run.");
        }
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

    private String buildRagContext(RagSearchResponse rag) {
        if (rag == null || rag.getHits() == null || rag.getHits().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rag.getHits().size(); i++) {
            RagSearchHit hit = rag.getHits().get(i);
            builder.append("[").append(i + 1).append("] ").append(hit.getCitation()).append("\n")
                    .append(firstText(hit.getContent(), hit.getContentPreview(), "")).append("\n");
        }
        return builder.toString();
    }

    private AiModelConfig resolveModel(AiChatRequest request) {
        if (request.getModelId() != null) {
            AiModelConfig byId = safeSelectById(modelConfigMapper, request.getModelId());
            if (byId != null) {
                return byId;
            }
        }
        if (StringUtils.hasText(request.getModelCode())) {
            AiModelConfig byCode = safeSelectOne(modelConfigMapper, new QueryWrapper<AiModelConfig>()
                    .eq("model_code", request.getModelCode())
                    .eq("status", "ENABLED")
                    .last("limit 1"));
            if (byCode != null) {
                return byCode;
            }
        }
        AiModelConfig defaultChat = safeSelectOne(modelConfigMapper, new QueryWrapper<AiModelConfig>()
                .eq("model_type", "CHAT")
                .eq("default_model", true)
                .eq("status", "ENABLED")
                .orderByAsc("sort_order")
                .last("limit 1"));
        if (defaultChat != null) {
            return defaultChat;
        }
        return safeSelectOne(modelConfigMapper, new QueryWrapper<AiModelConfig>()
                .eq("status", "ENABLED")
                .orderByAsc("sort_order")
                .last("limit 1"));
    }

    private AiChatBiDataset resolveDataset(ChatBiAskRequest request) {
        if (request.getDatasetId() != null) {
            AiChatBiDataset dataset = safeSelectById(chatBiDatasetMapper, request.getDatasetId());
            if (dataset != null) {
                return dataset;
            }
        }
        if (StringUtils.hasText(request.getDatasetCode())) {
            AiChatBiDataset dataset = safeSelectOne(chatBiDatasetMapper, new QueryWrapper<AiChatBiDataset>()
                    .eq("dataset_code", request.getDatasetCode())
                    .eq("status", "ENABLED")
                    .last("limit 1"));
            if (dataset != null) {
                return dataset;
            }
        }
        return safeSelectOne(chatBiDatasetMapper, new QueryWrapper<AiChatBiDataset>()
                .eq("status", "ENABLED")
                .orderByDesc("create_time")
                .last("limit 1"));
    }

    private List<AiChatBiTable> resolveTables(AiChatBiDataset dataset, Long datasourceId) {
        List<Long> tableIds = parseLongList(dataset == null ? null : dataset.getTableIds());
        if (!tableIds.isEmpty()) {
            return new ArrayList<>(selectByIds(chatBiTableMapper, tableIds).values());
        }
        if (datasourceId != null) {
            return safeSelectList(chatBiTableMapper, new QueryWrapper<AiChatBiTable>()
                    .eq("datasource_id", datasourceId)
                    .eq("status", "ENABLED")
                    .orderByAsc("table_id")
                    .last("limit 20"));
        }
        return List.of();
    }

    private List<AiChatBiTerm> resolveTerms(AiChatBiDataset dataset) {
        QueryWrapper<AiChatBiTerm> wrapper = new QueryWrapper<AiChatBiTerm>().eq("status", "ENABLED");
        if (dataset != null) {
            wrapper.and(nested -> nested.eq("dataset_id", dataset.getDatasetId()).or().isNull("dataset_id"));
        }
        return safeSelectList(chatBiTermMapper, wrapper.orderByDesc("create_time").last("limit 100"));
    }

    private ChatBiTableSnapshot toTableSnapshot(AiChatBiTable table) {
        ChatBiTableSnapshot snapshot = new ChatBiTableSnapshot();
        snapshot.setTableId(table.getTableId());
        snapshot.setDatasourceId(table.getDatasourceId());
        snapshot.setSchemaName(table.getSchemaName());
        snapshot.setTableName(table.getTableName());
        snapshot.setTableComment(table.getTableComment());
        snapshot.setColumns(parseColumns(table.getColumnsJson()));
        return snapshot;
    }

    private String buildChatBiExplanation(AiChatBiDataset dataset, AiChatBiDatasource datasource,
                                          List<AiChatBiTable> tables, List<ChatBiTermMatch> matchedTerms) {
        return "基于数据集 "
                + firstText(dataset == null ? null : dataset.getDatasetName(), "未指定")
                + "、数据源 "
                + firstText(datasource == null ? null : datasource.getDatasourceName(), "未指定")
                + "、" + tables.size() + " 张表和 " + matchedTerms.size()
                + " 个术语匹配生成只读 SELECT 草案；接口不会执行 SQL。";
    }

    private void recordBilling(String conversationId, String provider, String modelCode, String bizType,
                               AiTokenUsageVO usage, String finishReason) {
        try {
            AiBillingRecord record = new AiBillingRecord();
            record.setConversationId(conversationId);
            record.setProvider(provider);
            record.setModelCode(modelCode);
            record.setBizType(bizType);
            record.setPromptTokens(usage == null ? 0 : usage.getPromptTokens());
            record.setCompletionTokens(usage == null ? 0 : usage.getCompletionTokens());
            record.setTotalTokens(usage == null ? 0 : usage.getTotalTokens());
            record.setCostAmount(BigDecimal.ZERO);
            record.setCurrency("CNY");
            record.setRequestTime(LocalDateTime.now());
            record.setStatus("SUCCESS");
            record.setRemark("ai-runtime finishReason=" + firstText(finishReason, "unknown"));
            billingRecordMapper.insert(record);
        } catch (RuntimeException ex) {
            if (!isMissingTable(ex)) {
                throw ex;
            }
        }
    }

    private List<JsonNode> graphNodes(JsonNode graph) {
        JsonNode nodesNode = graph.path("nodes");
        if (!nodesNode.isArray()) {
            return List.of();
        }
        List<JsonNode> nodes = new ArrayList<>();
        nodesNode.forEach(nodes::add);
        return nodes;
    }

    private List<JsonNode> defaultWorkflowNodes() {
        try {
            return graphNodes(objectMapper.readTree("""
                    {"nodes":[
                      {"id":"start","type":"start","name":"Start"},
                      {"id":"rag","type":"rag","name":"RAG Search"},
                      {"id":"summary","type":"llm","name":"LLM Summary"}
                    ],"edges":[["start","rag"],["rag","summary"]]}
                    """));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String workflowSummary(AiWorkflow workflow, List<WorkflowNodeTrace> traces, Object lastOutput) {
        long successCount = traces.stream().filter(trace -> "SUCCESS".equals(trace.getStatus())).count();
        return "Workflow " + workflow.getWorkflowName() + " executed " + traces.size()
                + " nodes, " + successCount + " succeeded. Last output: " + preview(toJsonString(lastOutput), 300);
    }

    private JsonNode parseGraph(String graphJson) {
        if (!StringUtils.hasText(graphJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(graphJson);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private List<Map<String, Object>> parseColumns(String columnsJson) {
        if (!StringUtils.hasText(columnsJson)) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(columnsJson);
            JsonNode columnsNode = node.isArray() ? node : node.path("columns");
            if (!columnsNode.isArray()) {
                return List.of(Map.of("raw", columnsJson));
            }
            return objectMapper.convertValue(columnsNode, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception ex) {
            return List.of(Map.of("raw", columnsJson));
        }
    }

    private Map<String, Object> parseJson(String text) {
        if (!StringUtils.hasText(text)) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(text);
            if (node.isObject()) {
                return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
                });
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", objectMapper.convertValue(node, Object.class));
            return map;
        } catch (Exception ex) {
            return Map.of("raw", text);
        }
    }

    private String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String selectColumns(AiChatBiTable table) {
        List<Map<String, Object>> columns = parseColumns(table.getColumnsJson());
        List<String> names = columns.stream()
                .map(column -> asString(column.get("name")))
                .filter(this::isSafeIdentifier)
                .limit(8)
                .toList();
        if (names.isEmpty()) {
            return "*";
        }
        return String.join(", ", names);
    }

    private String tableReference(AiChatBiTable table) {
        if (table == null || !isSafeIdentifier(table.getTableName())) {
            return null;
        }
        if (StringUtils.hasText(table.getSchemaName())) {
            if (!isSafeIdentifier(table.getSchemaName())) {
                return null;
            }
            return table.getSchemaName() + "." + table.getTableName();
        }
        return table.getTableName();
    }

    private boolean isSafeReadExpression(String expression) {
        if (!StringUtils.hasText(expression) || expression.contains(";") || MUTATION_SQL.matcher(expression).find()) {
            return false;
        }
        return expression.matches("[A-Za-z0-9_.*(),\\s+\\-/=<>]+");
    }

    private String safeFilter(String filter, List<String> warnings) {
        if (!StringUtils.hasText(filter)) {
            return null;
        }
        if (filter.contains(";") || MUTATION_SQL.matcher(filter).find()) {
            warnings.add("Dataset default filter contains unsafe SQL keywords and was ignored.");
            return null;
        }
        return filter;
    }

    private String safeAlias(String alias) {
        String normalized = alias == null ? "metric_value" : alias.replaceAll("[^A-Za-z0-9_]", "_");
        if (!SAFE_IDENTIFIER.matcher(normalized).matches()) {
            return "metric_value";
        }
        return normalized;
    }

    private boolean isSafeIdentifier(String text) {
        return StringUtils.hasText(text) && SAFE_IDENTIFIER.matcher(text).matches();
    }

    private String nodeName(JsonNode node) {
        String name = nodeText(node, "name", null);
        if (StringUtils.hasText(name)) {
            return name;
        }
        name = nodeText(node, "label", null);
        if (StringUtils.hasText(name)) {
            return name;
        }
        JsonNode data = node.path("data");
        return firstText(data.path("name").asText(null), data.path("label").asText(null), nodeText(node, "id", "node"));
    }

    private String nodeText(JsonNode node, String field, String defaultValue) {
        if (node.has(field) && !node.path(field).isNull()) {
            return node.path(field).asText(defaultValue);
        }
        JsonNode data = node.path("data");
        if (data.has(field) && !data.path(field).isNull()) {
            return data.path(field).asText(defaultValue);
        }
        return defaultValue;
    }

    private Long nodeLong(JsonNode node, String field) {
        if (node.has(field) && node.path(field).canConvertToLong()) {
            return node.path(field).asLong();
        }
        JsonNode data = node.path("data");
        if (data.has(field) && data.path(field).canConvertToLong()) {
            return data.path(field).asLong();
        }
        return null;
    }

    private String citation(AiKnowledgeBase knowledgeBase, AiDocument document, AiDocumentChunk chunk) {
        return firstText(knowledgeBase == null ? null : knowledgeBase.getKnowledgeBaseName(), "KB#" + chunk.getKnowledgeBaseId())
                + " / "
                + firstText(document == null ? null : document.getDocumentName(), "DOC#" + chunk.getDocumentId())
                + " #chunk-" + firstText(chunk.getChunkIndex() == null ? null : String.valueOf(chunk.getChunkIndex()), String.valueOf(chunk.getChunkId()));
    }

    private String resolveApiKey(String apiKeyRef) {
        if (!StringUtils.hasText(apiKeyRef)) {
            return null;
        }
        String ref = apiKeyRef.trim();
        if (ref.startsWith("ENV:")) {
            return lookupSecret(ref.substring(4));
        }
        if (ref.startsWith("PROP:")) {
            return environment.getProperty(ref.substring(5));
        }
        if (ref.startsWith("${") && ref.endsWith("}")) {
            return lookupSecret(ref.substring(2, ref.length() - 1));
        }
        if (ref.startsWith("VALUE:")) {
            return ref.substring(6);
        }
        return lookupSecret(ref);
    }

    private String lookupSecret(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String value = environment.getProperty(name);
        if (!StringUtils.hasText(value)) {
            value = System.getenv(name);
        }
        return StringUtils.hasText(value) ? value : null;
    }

    private String completionUrl(String endpoint) {
        String trimmed = endpoint.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        }
        return trimmed + "/v1/chat/completions";
    }

    private String missingProviderReason(AiModelConfig model, String apiKey) {
        if (model == null) {
            return "no enabled chat model configuration was found";
        }
        if (!StringUtils.hasText(model.getEndpoint())) {
            return "model endpoint is not configured";
        }
        if (!StringUtils.hasText(apiKey)) {
            return "apiKeyRef did not resolve to an environment/property value";
        }
        return "provider call skipped";
    }

    private boolean isRagEnabled(Map<String, Object> options) {
        Object rag = options.get("rag");
        Object useRag = options.get("useRag");
        return !Boolean.FALSE.equals(toBoolean(rag)) && !Boolean.FALSE.equals(toBoolean(useRag));
    }

    private AiTokenUsageVO estimateUsage(String prompt, String answer) {
        int promptTokens = estimateTokens(prompt);
        int completionTokens = estimateTokens(answer);
        return new AiTokenUsageVO(promptTokens, completionTokens, promptTokens + completionTokens);
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private String latestUserText(List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessage message = messages.get(i);
            if (message != null && "user".equalsIgnoreCase(message.getRole())) {
                return message.getContent();
            }
        }
        AiChatMessage last = messages.get(messages.size() - 1);
        return last == null ? "" : last.getContent();
    }

    private List<String> searchTerms(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String part : SPLIT_PATTERN.split(query.trim())) {
            if (StringUtils.hasText(part)) {
                terms.add(part.trim());
            }
        }
        if (terms.isEmpty()) {
            terms.add(query.trim());
        }
        return new ArrayList<>(terms);
    }

    private List<Long> effectiveKnowledgeBaseIds(RagSearchRequest request) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (request.getKnowledgeBaseId() != null) {
            ids.add(request.getKnowledgeBaseId());
        }
        if (request.getKnowledgeBaseIds() != null) {
            ids.addAll(request.getKnowledgeBaseIds().stream().filter(Objects::nonNull).toList());
        }
        return new ArrayList<>(ids);
    }

    private Map<String, Object> safeOptions(Map<String, Object> options) {
        return options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    private List<String> parseTokens(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("[,，\\s]+")) {
            if (StringUtils.hasText(token)) {
                tokens.add(token.trim());
            }
        }
        return tokens;
    }

    private List<Long> parseLongList(String value) {
        return parseTokens(value).stream().map(this::toLong).filter(Objects::nonNull).toList();
    }

    private List<Long> toLongList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::toLong).filter(Objects::nonNull).toList();
        }
        return parseLongList(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = String.valueOf(value).trim();
            if (text.startsWith("api:") || text.startsWith("mcp:")) {
                text = text.substring(4);
            }
            return StringUtils.hasText(text) ? Long.parseLong(text) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long firstLong(Long first, Long second) {
        return first == null ? second : first;
    }

    private Integer toInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int clamp(Integer value, int defaultValue, int min, int max) {
        int safeValue = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, safeValue));
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String lower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private String preview(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength) + "...";
    }

    private int countOccurrences(String text, String term) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(term)) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) >= 0) {
            count++;
            index += term.length();
        }
        return count;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private <T> T safeSelectById(BaseMapper<T> mapper, Serializable id) {
        try {
            return mapper.selectById(id);
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return null;
            }
            throw ex;
        }
    }

    private <T> T safeSelectOne(BaseMapper<T> mapper, QueryWrapper<T> wrapper) {
        try {
            return mapper.selectOne(wrapper);
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return null;
            }
            throw ex;
        }
    }

    private <T> List<T> safeSelectList(BaseMapper<T> mapper, QueryWrapper<T> wrapper) {
        try {
            return mapper.selectList(wrapper);
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return List.of();
            }
            throw ex;
        }
    }

    private <T> Map<Long, T> selectByIds(BaseMapper<T> mapper, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            return mapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(this::entityId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return Map.of();
            }
            throw ex;
        }
    }

    private Long entityId(Object entity) {
        if (entity instanceof AiDocument document) {
            return document.getDocumentId();
        }
        if (entity instanceof AiKnowledgeBase knowledgeBase) {
            return knowledgeBase.getKnowledgeBaseId();
        }
        if (entity instanceof AiChatBiTable table) {
            return table.getTableId();
        }
        return null;
    }

    private boolean isMissingTable(RuntimeException ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            String lower = message.toLowerCase(Locale.ROOT);
            if ((lower.contains("relation") || lower.contains("table"))
                    && (lower.contains("does not exist") || lower.contains("doesn't exist"))) {
                return true;
            }
        }
        return false;
    }

    private record ProviderResult(String content, String finishReason, AiTokenUsageVO usage, String error, boolean success) {

        private static ProviderResult ok(String content, String finishReason, AiTokenUsageVO usage) {
            return new ProviderResult(content, finishReason, usage, null, true);
        }

        private static ProviderResult failed(String error) {
            return new ProviderResult(null, null, null, error, false);
        }

        private static ProviderResult skipped(String reason) {
            return failed(reason);
        }
    }
}
