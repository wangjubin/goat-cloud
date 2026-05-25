package com.goat.cloud.module.ai.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.module.ai.entity.AiIntentConfig;
import com.goat.cloud.module.ai.entity.AiStateGraph;
import com.goat.cloud.module.ai.entity.AiStateNode;
import com.goat.cloud.module.ai.mapper.AiIntentConfigMapper;
import com.goat.cloud.module.ai.mapper.AiStateGraphMapper;
import com.goat.cloud.module.ai.mapper.AiStateNodeMapper;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.runtime.model.AgentRunRequest;
import com.goat.cloud.module.ai.runtime.model.AgentRunResponse;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunRequest;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunResponse;
import com.goat.cloud.module.ai.runtime.model.WorkflowNodeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiWorkflowService {

    private final ObjectMapper objectMapper;
    private final AiStateGraphMapper stateGraphMapper;
    private final AiStateNodeMapper stateNodeMapper;
    private final AiIntentConfigMapper intentConfigMapper;
    private final AiRagSearchService ragSearchService;
    private final AiChatBiService chatBiService;
    private final AiAgentService agentService;
    private final AiChatService chatService;

    public WorkflowRunResponse runWorkflow(Long workflowId, WorkflowRunRequest request) {
        if (workflowId == null) {
            throw new BusinessException(4001, "Workflow id is required");
        }
        AiStateGraph graph = safeSelectById(stateGraphMapper, workflowId);
        if (graph == null) {
            throw new BusinessException(4044, "Workflow not found");
        }
        WorkflowRunRequest safeRequest = request == null ? new WorkflowRunRequest() : request;
        String conversationId = StringUtils.hasText(safeRequest.getConversationId())
                ? safeRequest.getConversationId()
                : UUID.randomUUID().toString();
        Map<String, Object> options = safeOptions(safeRequest.getOptions());
        List<AiStateNode> nodes = resolveNodes(graph);
        AiIntentConfig intent = resolveIntent(graph);
        Map<String, Object> variables = safeRequest.getVariables() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(safeRequest.getVariables());
        if (StringUtils.hasText(safeRequest.getMessage())) {
            variables.put("userMessage", safeRequest.getMessage());
        }

        List<WorkflowNodeResult> nodeResults = new ArrayList<>();
        Map<String, Object> context = new LinkedHashMap<>(variables);
        for (AiStateNode node : nodes) {
            WorkflowNodeResult nodeResult = executeWorkflowNode(node, graph, intent, safeRequest, context, options, conversationId);
            nodeResults.add(nodeResult);
            if (nodeResult.getOutput() != null) {
                context.put(node.getNodeCode() != null ? node.getNodeCode() : "node-" + node.getNodeId(), nodeResult.getOutput());
            }
            context.put("_lastNodeResult", nodeResult);
            if ("FAILED".equals(nodeResult.getStatus())) break;
        }

        WorkflowRunResponse response = new WorkflowRunResponse();
        response.setWorkflowId(graph.getGraphId());
        response.setWorkflowCode(graph.getGraphCode());
        response.setWorkflowName(graph.getGraphName());
        response.setDescription(graph.getDescription());
        response.setConversationId(conversationId);
        response.setVariables(variables);
        response.setNodeResults(nodeResults);
        response.setFinalOutput(context.get("_lastNodeResult") instanceof WorkflowNodeResult last ? last.getOutput() : null);
        response.setComplete(nodeResults.stream().noneMatch(r -> "FAILED".equals(r.getStatus())));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nodeCount", nodes.size());
        metadata.put("enabledNodeCount", nodeResults.size());
        metadata.put("intentConfigured", intent != null);
        metadata.put("context", context);
        metadata.put("runtime", Map.of("mode", "workflow-linear-execute", "graphVersion", graph.getVersion()));
        response.setMetadata(metadata);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private WorkflowNodeResult executeWorkflowNode(AiStateNode node, AiStateGraph graph, AiIntentConfig intent,
                                                   WorkflowRunRequest request, Map<String, Object> context,
                                                   Map<String, Object> options, String conversationId) {
        WorkflowNodeResult result = new WorkflowNodeResult();
        result.setNodeId(node.getNodeId());
        result.setNodeCode(node.getNodeCode());
        result.setNodeName(node.getNodeName());
        result.setNodeType(node.getNodeType());
        result.setStartedAt(LocalDateTime.now());
        try {
            Map<String, Object> output = switch (AiRuntimeHelper.firstText(node.getNodeType(), "passthrough")) {
                case "rag-search" -> executeRagNode(node, request, context, options);
                case "chatbi" -> executeChatBiNode(node, request, context, options);
                case "agent" -> executeAgentNode(node, request, context, options, conversationId);
                case "chat" -> executeChatNode(node, graph, intent, request, context, options, conversationId);
                case "intent-classify" -> executeIntentNode(node, intent, request, context);
                case "transform" -> executeTransformNode(node, context);
                default -> executePassthroughNode(node, context);
            };
            result.setStatus("SUCCESS");
            result.setOutput(output);
        } catch (Exception ex) {
            result.setStatus("FAILED");
            result.setOutput(Map.of("error", ex.getMessage()));
        }
        result.setFinishedAt(LocalDateTime.now());
        return result;
    }

    private Map<String, Object> executeRagNode(AiStateNode node, WorkflowRunRequest request,
                                               Map<String, Object> context, Map<String, Object> options) {
        String query = AiRuntimeHelper.firstText(AiRuntimeHelper.asString(context.get("userMessage")), AiRuntimeHelper.asString(context.get("query")), "");
        com.goat.cloud.module.ai.runtime.model.RagSearchRequest ragRequest = new com.goat.cloud.module.ai.runtime.model.RagSearchRequest();
        ragRequest.setQuery(query);
        ragRequest.setTopK(AiRuntimeHelper.toInteger(options.get("topK"), 5));
        ragRequest.setIncludeContent(Boolean.TRUE);
        ragRequest.setKnowledgeBaseId(AiRuntimeHelper.toLong(options.get("knowledgeBaseId")));
        com.goat.cloud.module.ai.runtime.model.RagSearchResponse rag = ragSearchService.search(ragRequest);
        return Map.of(
                "hits", rag.getHits(),
                "total", rag.getTotal(),
                "context", ragSearchService.buildRagContext(rag)
        );
    }

    private Map<String, Object> executeChatBiNode(AiStateNode node, WorkflowRunRequest request,
                                                  Map<String, Object> context, Map<String, Object> options) {
        com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest chatBiRequest = new com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest();
        chatBiRequest.setQuestion(AiRuntimeHelper.firstText(AiRuntimeHelper.asString(context.get("userMessage")), ""));
        chatBiRequest.setDatasetId(AiRuntimeHelper.toLong(options.get("chatBiDatasetId")));
        chatBiRequest.setDatasourceId(AiRuntimeHelper.toLong(options.get("chatBiDatasourceId")));
        chatBiRequest.setLimit(AiRuntimeHelper.toInteger(options.get("chatBiLimit"), 100));
        com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse chatBi = chatBiService.askChatBi(chatBiRequest);
        return Map.of(
                "candidateSql", chatBi.getCandidateSql() == null ? "" : chatBi.getCandidateSql(),
                "executionPolicy", chatBi.getExecutionPolicy() == null ? "" : chatBi.getExecutionPolicy(),
                "warnings", chatBi.getSafetyWarnings() == null ? List.of() : chatBi.getSafetyWarnings(),
                "explanation", chatBi.getExplanation() == null ? "" : chatBi.getExplanation()
        );
    }

    private Map<String, Object> executeAgentNode(AiStateNode node, WorkflowRunRequest request,
                                                 Map<String, Object> context, Map<String, Object> options,
                                                 String conversationId) {
        Long agentId = AiRuntimeHelper.toLong(options.get("agentId"));
        if (agentId == null) agentId = AiRuntimeHelper.toLong(node.getConfigJson());
        if (agentId == null) return Map.of("status", "SKIPPED", "reason", "No agentId configured for this node.");
        AgentRunRequest agentRequest = new AgentRunRequest();
        agentRequest.setMessage(AiRuntimeHelper.firstText(AiRuntimeHelper.asString(context.get("userMessage")), ""));
        agentRequest.setConversationId(conversationId);
        agentRequest.setVariables(context);
        agentRequest.setOptions(options);
        AgentRunResponse agent = agentService.runAgent(agentId, agentRequest);
        return Map.of("agentId", agent.getAgentId(), "agentCode", agent.getAgentCode(), "chat", agent.getChat());
    }

    private Map<String, Object> executeChatNode(AiStateNode node, AiStateGraph graph, AiIntentConfig intent,
                                                WorkflowRunRequest request, Map<String, Object> context,
                                                Map<String, Object> options, String conversationId) {
        AiChatRequest chatRequest = new AiChatRequest();
        chatRequest.setConversationId(conversationId);
        chatRequest.setModelId(extractModelId(graph.getConfigJson()));
        chatRequest.setMessage(AiRuntimeHelper.firstText(AiRuntimeHelper.asString(context.get("userMessage")), ""));
        String systemPrompt = AiRuntimeHelper.firstText(extractSystemPrompt(graph.getConfigJson()), "You are a Goat Cloud workflow assistant.");
        if (intent != null && StringUtils.hasText(intent.getPromptTemplate())) {
            systemPrompt = intent.getPromptTemplate();
        }
        chatRequest.setSystemPrompt(systemPrompt);
        String ragContext = AiRuntimeHelper.asString(context.get("context"));
        if (StringUtils.hasText(ragContext)) {
            options.put("_injectedRagContext", ragContext);
        }
        options.put("bizType", "WORKFLOW");
        options.put("workflowId", graph.getGraphId());
        options.put("rag", Boolean.FALSE);
        chatRequest.setOptions(options);
        AiChatResponse chat = chatService.chat(chatRequest);
        return Map.of("content", chat.getMessage() == null ? "" : chat.getMessage().getContent(), "usage", chat.getUsage(), "mock", chat.getMock());
    }

    private Map<String, Object> executeIntentNode(AiStateNode node, AiIntentConfig intent,
                                                  WorkflowRunRequest request, Map<String, Object> context) {
        if (intent == null) {
            return Map.of("status", "SKIPPED", "reason", "No intent configuration linked to this workflow.");
        }
        String message = AiRuntimeHelper.firstText(AiRuntimeHelper.asString(context.get("userMessage")), "");
        return Map.of(
                "intentId", intent.getConfigId(),
                "intentName", intent.getIntentName(),
                "keywords", parseJson(intent.getExamplesJson()),
                "detectedIntent", detectIntent(message, intent)
        );
    }

    private Map<String, Object> executeTransformNode(AiStateNode node, Map<String, Object> context) {
        return Map.of("transform", "passthrough", "inputKeys", new ArrayList<>(context.keySet()), "outputKeys", new ArrayList<>(context.keySet()));
    }

    private Map<String, Object> executePassthroughNode(AiStateNode node, Map<String, Object> context) {
        return Map.of("passthrough", true, "contextKeys", new ArrayList<>(context.keySet()));
    }

    private String detectIntent(String message, AiIntentConfig intent) {
        if (!StringUtils.hasText(message)) return "unknown";
        String keywords = intent.getExamplesJson();
        if (!StringUtils.hasText(keywords)) return "unmatched";
        try {
            JsonNode node = objectMapper.readTree(keywords);
            if (node.isArray()) {
                for (JsonNode entry : node) {
                    String keyword = entry.path("keyword").asText("");
                    if (StringUtils.hasText(keyword) && AiRuntimeHelper.lower(message).contains(AiRuntimeHelper.lower(keyword))) {
                        return entry.path("intentCode").asText(entry.path("intent").asText("matched"));
                    }
                }
            }
        } catch (Exception ignored) {}
        return "unmatched";
    }

    private List<AiStateNode> resolveNodes(AiStateGraph graph) {
        if (graph == null) return List.of();
        try {
            return stateNodeMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiStateNode>()
                    .eq("graph_id", graph.getGraphId())
                    .orderByAsc("sort_order"));
        } catch (RuntimeException ex) {
            if (AiRuntimeHelper.isMissingTable(ex)) return List.of();
            throw ex;
        }
    }

    private AiIntentConfig resolveIntent(AiStateGraph graph) {
        Long intentConfigId = extractIntentConfigId(graph.getConfigJson());
        if (graph == null || intentConfigId == null) return null;
        try {
            return intentConfigMapper.selectById(intentConfigId);
        } catch (RuntimeException ex) {
            if (AiRuntimeHelper.isMissingTable(ex)) return null;
            throw ex;
        }
    }

    private Long extractModelId(String configJson) {
        return AiRuntimeHelper.toLong(parseJson(configJson).get("modelId"));
    }

    private String extractSystemPrompt(String configJson) {
        Object prompt = parseJson(configJson).get("systemPrompt");
        return prompt == null ? null : String.valueOf(prompt);
    }

    private Long extractIntentConfigId(String configJson) {
        return AiRuntimeHelper.toLong(parseJson(configJson).get("intentConfigId"));
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

    private Map<String, Object> safeOptions(Map<String, Object> options) {
        return options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    private <T> T safeSelectById(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, java.io.Serializable id) {
        try { return mapper.selectById(id); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return null; throw ex; }
    }
}