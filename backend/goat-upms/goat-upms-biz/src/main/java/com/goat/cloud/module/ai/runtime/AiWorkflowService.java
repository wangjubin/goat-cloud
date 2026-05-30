package com.goat.cloud.module.ai.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.module.ai.entity.AiStateGraph;
import com.goat.cloud.module.ai.entity.AiStateSession;
import com.goat.cloud.module.ai.entity.AiWorkflow;
import com.goat.cloud.module.ai.mapper.AiStateGraphMapper;
import com.goat.cloud.module.ai.mapper.AiWorkflowMapper;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunRequest;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunResponse;
import com.goat.cloud.module.ai.runtime.model.WorkflowNodeResult;
import com.goat.cloud.module.ai.runtime.model.WorkflowNodeTrace;
import com.goat.cloud.module.ai.service.stategraph.StateExecutionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Workflow runner that delegates to the DAG-based StateExecutionEngine.
 * Maps Ask-style workflow requests/responses to the platform engine.
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiWorkflowService {

    private final ObjectMapper objectMapper;
    private final AiStateGraphMapper stateGraphMapper;
    private final AiWorkflowMapper workflowMapper;
    private final StateExecutionEngine executionEngine;

    /**
     * Run a workflow by delegating to StateExecutionEngine.
     * Falls back to simple linear execution if engine is unavailable.
     */
    public WorkflowRunResponse runWorkflow(Long workflowId, WorkflowRunRequest request) {
        if (workflowId == null) {
            throw new BusinessException(4001, "Workflow id is required");
        }
        AiStateGraph graph = safeSelectById(workflowId);
        if (graph == null) {
            // Fallback: try ai_workflow table and convert to a minimal AiStateGraph
            AiWorkflow workflow = safeSelectWorkflowById(workflowId);
            if (workflow == null) {
                throw new BusinessException(4044, "Workflow not found");
            }
            graph = new AiStateGraph();
            graph.setGraphId(workflow.getWorkflowId());
            graph.setGraphCode(workflow.getWorkflowCode());
            graph.setGraphName(workflow.getWorkflowName());
            graph.setDescription(workflow.getDescription());
            graph.setVersion(workflow.getVersion());
            graph.setDefinitionJson(workflow.getGraphJson());
            graph.setStatus(workflow.getStatus() != null ? workflow.getStatus().name() : "ENABLED");
        }

        WorkflowRunRequest safeRequest = request == null ? new WorkflowRunRequest() : request;
        String conversationId = StringUtils.hasText(safeRequest.getConversationId())
                ? safeRequest.getConversationId()
                : UUID.randomUUID().toString();

        Map<String, Object> input = new LinkedHashMap<>();
        if (StringUtils.hasText(safeRequest.getMessage())) {
            input.put("userMessage", safeRequest.getMessage());
        }
        if (safeRequest.getVariables() != null) {
            input.putAll(safeRequest.getVariables());
        }
        input.put("conversationId", conversationId);
        if (safeRequest.getOptions() != null) {
            input.putAll(safeRequest.getOptions());
        }

        // Delegate to the DAG engine
        try {
            AiStateSession session = executionEngine.startSession(graph.getGraphCode(), null, input);
            return mapToWorkflowResponse(graph, session, conversationId, safeRequest);
        } catch (Exception e) {
            log.warn("DAG engine execution failed for workflow {}, falling back: {}", workflowId, e.getMessage());
            // Fallback: return error response
            WorkflowRunResponse response = new WorkflowRunResponse();
            response.setWorkflowId(graph.getGraphId());
            response.setWorkflowCode(graph.getGraphCode());
            response.setWorkflowName(graph.getGraphName());
            response.setDescription(graph.getDescription());
            response.setConversationId(conversationId);
            response.setComplete(false);
            response.setVariables(safeRequest.getVariables());
            response.setFinalOutput(Map.of("error", e.getMessage()));
            response.setCreatedAt(LocalDateTime.now());
            return response;
        }
    }

    private WorkflowRunResponse mapToWorkflowResponse(AiStateGraph graph, AiStateSession session,
                                                       String conversationId, WorkflowRunRequest request) {
        WorkflowRunResponse response = new WorkflowRunResponse();
        response.setRunId(session.getRunId());
        response.setWorkflowId(graph.getGraphId());
        response.setWorkflowCode(graph.getGraphCode());
        response.setWorkflowName(graph.getGraphName());
        response.setDescription(graph.getDescription());
        response.setVersion(graph.getVersion());
        response.setConversationId(conversationId);
        response.setVariables(request != null ? request.getVariables() : new LinkedHashMap<>());
        response.setStartedAt(session.getStartedAt());
        response.setCompletedAt(session.getCompletedAt());
        response.setCreatedAt(LocalDateTime.now());

        // Map session status
        String sessionStatus = session.getStatus();
        response.setComplete("COMPLETED".equals(sessionStatus));
        response.setStatus(sessionStatus);

        // Map node traces to nodeResults
        List<WorkflowNodeResult> nodeResults = new ArrayList<>();
        List<WorkflowNodeTrace> traces = new ArrayList<>();
        // The session stores node traces in resultJson — parse if available
        String resultJson = session.getResultJson();
        if (StringUtils.hasText(resultJson)) {
            try {
                com.fasterxml.jackson.databind.JsonNode resultNode = objectMapper.readTree(resultJson);
                if (resultNode.has("nodeResults") && resultNode.get("nodeResults").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode nr : resultNode.get("nodeResults")) {
                        WorkflowNodeResult wnr = new WorkflowNodeResult();
                        wnr.setNodeCode(nr.path("nodeCode").asText(""));
                        wnr.setNodeName(nr.path("nodeName").asText(""));
                        wnr.setNodeType(nr.path("nodeType").asText(""));
                        wnr.setStatus(nr.path("status").asText("UNKNOWN"));
                        response.setFinalOutput(nr.path("output"));
                        nodeResults.add(wnr);
                    }
                } else {
                    // Result is a simple output
                    response.setFinalOutput(resultNode);
                    // Extract summary from finalOutput if present
                    if (resultNode.has("summary")) {
                        com.fasterxml.jackson.databind.JsonNode summaryNode = resultNode.get("summary");
                        if (summaryNode.has("content")) {
                            response.setSummary(summaryNode.get("content").asText(""));
                        } else if (summaryNode.isTextual()) {
                            response.setSummary(summaryNode.asText(""));
                        }
                    }
                }
                // Build traces from result keys
                resultNode.fieldNames().forEachRemaining(key -> {
                    com.fasterxml.jackson.databind.JsonNode nodeVal = resultNode.get(key);
                    if (nodeVal.isObject() && (nodeVal.has("status") || nodeVal.has("passthrough"))) {
                        WorkflowNodeTrace trace = new WorkflowNodeTrace();
                        trace.setNodeId(key);
                        trace.setStatus(nodeVal.path("status").asText(nodeVal.has("passthrough") ? "PASSED" : "UNKNOWN"));
                        traces.add(trace);
                    }
                });
            } catch (Exception e) {
                log.debug("Could not parse resultJson, using raw value");
                response.setFinalOutput(Map.of("raw", resultJson));
            }
        }
        response.setNodeResults(nodeResults);
        response.setTraces(traces);

        // Build metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("runId", session.getRunId());
        metadata.put("sessionStatus", sessionStatus);
        metadata.put("engineMode", "dag-delegate");
        metadata.put("graphVersion", graph.getVersion());
        response.setMetadata(metadata);

        return response;
    }

    private AiStateGraph safeSelectById(Long id) {
        try {
            return stateGraphMapper.selectById(id);
        } catch (RuntimeException ex) {
            if (AiRuntimeHelper.isMissingTable(ex)) return null;
            throw ex;
        }
    }

    private AiWorkflow safeSelectWorkflowById(Long id) {
        try {
            return workflowMapper.selectById(id);
        } catch (RuntimeException ex) {
            if (AiRuntimeHelper.isMissingTable(ex)) return null;
            throw ex;
        }
    }
}
