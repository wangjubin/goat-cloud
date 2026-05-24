package com.goat.cloud.module.ai.service.stategraph;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiStateGraph;
import com.goat.cloud.module.ai.entity.AiStateNode;
import com.goat.cloud.module.ai.entity.AiStateSession;
import com.goat.cloud.module.ai.entity.AiStateTrace;
import com.goat.cloud.module.ai.mapper.AiStateGraphMapper;
import com.goat.cloud.module.ai.mapper.AiStateNodeMapper;
import com.goat.cloud.module.ai.mapper.AiStateSessionMapper;
import com.goat.cloud.module.ai.mapper.AiStateTraceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateExecutionEngineImpl implements StateExecutionEngine {

    private final AiStateGraphMapper graphMapper;
    private final AiStateNodeMapper nodeMapper;
    private final AiStateSessionMapper sessionMapper;
    private final AiStateTraceMapper traceMapper;
    private final ObjectMapper objectMapper;

    private final Map<String, NodeExecutor> executorRegistry = new ConcurrentHashMap<>();

    @Override
    public void registerExecutor(NodeExecutor executor) {
        executorRegistry.put(executor.getNodeType(), executor);
        log.info("Registered node executor: {} -> {}", executor.getNodeType(), executor.getClass().getSimpleName());
    }

    @Override
    @Transactional
    public AiStateSession startSession(String graphCode, Long userId, Map<String, Object> input) {
        AiStateGraph graph = loadGraph(graphCode);
        List<AiStateNode> nodes = loadNodes(graph.getGraphId());
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Graph has no nodes: " + graphCode);
        }

        AiStateSession session = createSession(graph, userId, input);
        return executeGraph(session, graph, nodes, input, null);
    }

    @Override
    @Transactional
    public AiStateSession resumeSession(String runId, Map<String, Object> feedback) {
        AiStateSession session = loadSession(runId);
        if (!"INTERRUPTED".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not interrupted: " + runId);
        }

        Map<String, Object> context = fromJson(session.getContextJson());
        context.put("feedback", feedback);
        session.setContextJson(toJson(context));
        session.setStatus("RUNNING");
        sessionMapper.updateById(session);

        AiStateGraph graph = graphMapper.selectById(session.getGraphId());
        List<AiStateNode> nodes = loadNodes(graph.getGraphId());

        // Find nodes that haven't been executed yet
        Set<Long> executedNodeIds = getExecutedNodeIds(session.getSessionId());
        List<AiStateNode> remainingNodes = nodes.stream()
                .filter(n -> !executedNodeIds.contains(n.getNodeId()))
                .collect(Collectors.toList());

        return executeGraph(session, graph, remainingNodes, context, null);
    }

    @Override
    public AiStateSession getSession(String runId) {
        return loadSession(runId);
    }

    @Override
    public AiStateSession getSessionById(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    @Transactional
    public void cancelSession(String runId) {
        AiStateSession session = loadSession(runId);
        session.setStatus("CANCELLED");
        session.setCompletedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    public List<AiStateTrace> getSessionTraces(Long sessionId) {
        return traceMapper.selectList(
                new LambdaQueryWrapper<AiStateTrace>()
                        .eq(AiStateTrace::getSessionId, sessionId)
                        .orderByAsc(AiStateTrace::getTraceId)
        );
    }

    @Override
    @Transactional
    public AiStateSession executeStreaming(String graphCode, Long userId, Map<String, Object> input,
                                           StreamCallback callback) {
        AiStateGraph graph = loadGraph(graphCode);
        List<AiStateNode> nodes = loadNodes(graph.getGraphId());
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Graph has no nodes: " + graphCode);
        }

        AiStateSession session = createSession(graph, userId, input);
        return executeGraph(session, graph, nodes, new HashMap<>(input), callback);
    }

    // ========== Core Graph Execution ==========

    private AiStateSession executeGraph(AiStateSession session, AiStateGraph graph,
                                         List<AiStateNode> nodes, Map<String, Object> context,
                                         StreamCallback callback) {
        String graphType = graph.getGraphType() != null ? graph.getGraphType() : "SEQUENTIAL";

        return switch (graphType) {
            case "DAG" -> executeDag(session, nodes, context, callback);
            case "PARALLEL" -> executeParallel(session, nodes, context, callback);
            default -> executeSequential(session, nodes, context, callback);
        };
    }

    /**
     * Sequential execution: nodes execute in sort_order order
     */
    private AiStateSession executeSequential(AiStateSession session, List<AiStateNode> nodes,
                                              Map<String, Object> context, StreamCallback callback) {
        for (AiStateNode node : nodes) {
            ExecutionAction action = executeNode(session, node, context, callback);
            if (action == ExecutionAction.INTERRUPT || action == ExecutionAction.FAIL) {
                return session;
            }
        }
        completeSession(session, context);
        if (callback != null) callback.onComplete(session);
        return session;
    }

    /**
     * DAG execution: topological sort, supports conditional branching
     * <p>
     * Each node's edgesJson defines outgoing edges with optional conditions.
     * Gateway nodes (GATEWAY_AND, GATEWAY_OR, GATEWAY_XOR) control branching logic.
     */
    private AiStateSession executeDag(AiStateSession session, List<AiStateNode> nodes,
                                       Map<String, Object> context, StreamCallback callback) {
        // Build node index
        Map<String, AiStateNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(AiStateNode::getNodeCode, n -> n, (a, b) -> a));

        // Find start nodes (no incoming edges or explicitly marked as START)
        List<AiStateNode> startNodes = findStartNodes(nodes);
        if (startNodes.isEmpty()) {
            failSession(session, "No start node found in DAG");
            if (callback != null) callback.onComplete(session);
            return session;
        }

        // Track completed nodes for DAG scheduling
        Set<String> completedNodes = new HashSet<>();
        Set<String> failedNodes = new HashSet<>();
        Queue<String> readyQueue = new LinkedList<>();
        startNodes.forEach(n -> readyQueue.add(n.getNodeCode()));

        int maxIterations = nodes.size() * 2; // Safety limit
        int iterations = 0;

        while (!readyQueue.isEmpty() && iterations < maxIterations) {
            iterations++;
            String nodeCode = readyQueue.poll();
            AiStateNode node = nodeMap.get(nodeCode);
            if (node == null || completedNodes.contains(nodeCode) || failedNodes.contains(nodeCode)) {
                continue;
            }

            ExecutionAction action = executeNode(session, node, context, callback);

            if (action == ExecutionAction.FAIL) {
                failedNodes.add(nodeCode);
                continue;
            }

            if (action == ExecutionAction.INTERRUPT) {
                return session;
            }

            completedNodes.add(nodeCode);

            // Resolve next nodes from edges
            List<String> nextNodes = resolveNextNodes(node, context);
            for (String nextCode : nextNodes) {
                AiStateNode nextNode = nodeMap.get(nextCode);
                if (nextNode != null && isNodeReady(nextNode, completedNodes)) {
                    readyQueue.add(nextCode);
                }
            }
        }

        // Check if all nodes completed
        if (completedNodes.size() == nodes.size()) {
            completeSession(session, context);
        } else if (failedNodes.isEmpty()) {
            session.setStatus("COMPLETED");
            session.setCompletedAt(LocalDateTime.now());
            session.setResultJson(toJson(context));
            session.setContextJson(toJson(context));
            sessionMapper.updateById(session);
        } else {
            failSession(session, "Some nodes failed: " + failedNodes);
        }

        if (callback != null) callback.onComplete(session);
        return session;
    }

    /**
     * Parallel execution: all nodes execute concurrently (simulated sequentially)
     */
    private AiStateSession executeParallel(AiStateSession session, List<AiStateNode> nodes,
                                            Map<String, Object> context, StreamCallback callback) {
        List<Map<String, Object>> results = new ArrayList<>();
        boolean anyFailed = false;

        for (AiStateNode node : nodes) {
            Map<String, Object> nodeContext = new HashMap<>(context);
            ExecutionAction action = executeNode(session, node, nodeContext, callback);
            if (action == ExecutionAction.FAIL) {
                anyFailed = true;
            } else if (action == ExecutionAction.INTERRUPT) {
                return session;
            }
            results.add(Map.of("nodeCode", node.getNodeCode(), "output", nodeContext.get(node.getNodeCode())));
        }

        // Merge all parallel results
        context.put("parallelResults", results);

        if (anyFailed) {
            session.setStatus("PARTIAL_COMPLETED");
        } else {
            completeSession(session, context);
        }
        if (callback != null) callback.onComplete(session);
        return session;
    }

    // ========== Node Execution ==========

    private ExecutionAction executeNode(AiStateSession session, AiStateNode node,
                                         Map<String, Object> context, StreamCallback callback) {
        NodeExecutor executor = executorRegistry.get(node.getNodeType());
        if (executor == null) {
            String error = "No executor registered for node type: " + node.getNodeType();
            log.error(error);
            if (callback != null) callback.onNodeError(node.getNodeCode(), node.getNodeType(), error);
            failSession(session, error);
            return ExecutionAction.FAIL;
        }

        if (callback != null) callback.onNodeStart(node.getNodeCode(), node.getNodeType());
        AiStateTrace trace = createTrace(session.getSessionId(), node);
        long startTime = System.currentTimeMillis();

        try {
            NodeResult result = executor.execute(context, node.getConfigJson());
            long duration = System.currentTimeMillis() - startTime;
            trace.setCompletedAt(LocalDateTime.now());
            trace.setDurationMs(duration);

            if (result.success()) {
                trace.setStatus("COMPLETED");
                trace.setOutputJson(result.outputJson());
                context.put(node.getNodeCode(), fromJsonOptional(result.outputJson()));
                if (callback != null) callback.onNodeComplete(node.getNodeCode(), node.getNodeType(), result.outputJson());

                if (result.shouldInterrupt()) {
                    session.setStatus("INTERRUPTED");
                    session.setCurrentNodeId(node.getNodeId());
                    session.setInterruptReason("HITL:" + node.getNodeCode());
                    session.setInterruptData(result.outputJson());
                    session.setInterruptedAt(LocalDateTime.now());
                    session.setContextJson(toJson(context));
                    sessionMapper.updateById(session);
                    traceMapper.insert(trace);
                    if (callback != null) callback.onInterrupt(node.getNodeCode(), "HITL:" + node.getNodeCode());
                    return ExecutionAction.INTERRUPT;
                }
                return ExecutionAction.CONTINUE;
            } else {
                trace.setStatus("FAILED");
                trace.setErrorMessage(result.errorMessage());
                if (callback != null) callback.onNodeError(node.getNodeCode(), node.getNodeType(), result.errorMessage());
                return ExecutionAction.FAIL;
            }
        } catch (Exception e) {
            trace.setStatus("FAILED");
            trace.setErrorMessage(e.getMessage());
            if (callback != null) callback.onNodeError(node.getNodeCode(), node.getNodeType(), e.getMessage());
            return ExecutionAction.FAIL;
        } finally {
            traceMapper.insert(trace);
        }
    }

    // ========== DAG Helpers ==========

    private List<AiStateNode> findStartNodes(List<AiStateNode> nodes) {
        // Nodes with type START are explicit start nodes
        List<AiStateNode> startNodes = nodes.stream()
                .filter(n -> "START".equals(n.getNodeType()))
                .collect(Collectors.toList());

        if (!startNodes.isEmpty()) return startNodes;

        // Fallback: first node by sort_order
        return nodes.stream()
                .filter(n -> n.getSortOrder() != null && n.getSortOrder() == 1)
                .collect(Collectors.toList());
    }

    /**
     * Resolve next nodes from a node's outgoing edges
     */
    private List<String> resolveNextNodes(AiStateNode node, Map<String, Object> context) {
        List<String> nextNodes = new ArrayList<>();

        if (node.getEdgesJson() != null && !node.getEdgesJson().isBlank()) {
            try {
                Map<String, Object> edges = objectMapper.readValue(node.getEdgesJson(), new TypeReference<>() {});
                Object outgoing = edges.get("outgoing");
                if (outgoing instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> edge) {
                            String to = (String) edge.get("to");
                            String condition = (String) edge.get("condition");
                            if (to != null && (condition == null || evaluateCondition(condition, context))) {
                                nextNodes.add(to);
                            }
                        } else if (item instanceof String s) {
                            nextNodes.add(s);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse edges for node {}: {}", node.getNodeCode(), e.getMessage());
            }
        }

        // Fallback: if no edges defined, use definition_json from the graph
        if (nextNodes.isEmpty()) {
            nextNodes = resolveFromGraphDefinition(node.getNodeCode(), context);
        }

        return nextNodes;
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveFromGraphDefinition(String nodeCode, Map<String, Object> context) {
        // Try to find edges from graph definition in context
        Object graphDef = context.get("graphDefinition");
        if (graphDef instanceof Map<?, ?> def) {
            Object edgesObj = def.get("edges");
            if (edgesObj instanceof List<?> edges) {
                return edges.stream()
                        .filter(e -> e instanceof Map<?, ?>)
                        .map(e -> (Map<String, Object>) e)
                        .filter(e -> nodeCode.equals(e.get("from")))
                        .map(e -> (String) e.get("to"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    /**
     * Check if all incoming edges for a node have been completed
     */
    private boolean isNodeReady(AiStateNode node, Set<String> completedNodes) {
        if (node.getEdgesJson() != null && !node.getEdgesJson().isBlank()) {
            try {
                Map<String, Object> edges = objectMapper.readValue(node.getEdgesJson(), new TypeReference<>() {});
                Object incoming = edges.get("incoming");
                if (incoming instanceof List<?> list) {
                    return list.stream().allMatch(in -> completedNodes.contains(String.valueOf(in)));
                }
            } catch (Exception e) {
                log.warn("Failed to parse incoming edges for node {}: {}", node.getNodeCode(), e.getMessage());
            }
        }
        return true;
    }

    /**
     * Simple condition evaluation for edge routing
     * Supports: "intent==TREND_ANALYSIS", "approved==true", "score>0.5"
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isBlank()) return true;
        try {
            String trimmed = condition.trim();
            if (trimmed.equals("true") || trimmed.equals("always")) return true;
            if (trimmed.equals("false") || trimmed.equals("never")) return false;

            // Simple key==value check
            if (trimmed.contains("==")) {
                String[] parts = trimmed.split("==", 2);
                String key = parts[0].trim();
                String expected = parts[1].trim();
                Object actual = context.get(key);
                return actual != null && String.valueOf(actual).equals(expected);
            }
            if (trimmed.contains("!=")) {
                String[] parts = trimmed.split("!=", 2);
                String key = parts[0].trim();
                String expected = parts[1].trim();
                Object actual = context.get(key);
                return actual == null || !String.valueOf(actual).equals(expected);
            }
            return true;
        } catch (Exception e) {
            log.warn("Condition evaluation failed for '{}': {}", condition, e.getMessage());
            return true;
        }
    }

    // ========== Session Helpers ==========

    private AiStateSession createSession(AiStateGraph graph, Long userId, Map<String, Object> input) {
        AiStateSession session = new AiStateSession();
        session.setGraphId(graph.getGraphId());
        session.setRunId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setStatus("RUNNING");
        session.setStartedAt(LocalDateTime.now());

        Map<String, Object> ctx = new HashMap<>(input);
        ctx.put("graphCode", graph.getGraphCode());
        ctx.put("graphType", graph.getGraphType());
        ctx.put("graphDefinition", parseJsonToMap(graph.getDefinitionJson()));
        session.setContextJson(toJson(ctx));
        sessionMapper.insert(session);
        return session;
    }

    private void completeSession(AiStateSession session, Map<String, Object> context) {
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        session.setResultJson(toJson(context));
        session.setContextJson(toJson(context));
        sessionMapper.updateById(session);
    }

    private Set<Long> getExecutedNodeIds(Long sessionId) {
        return traceMapper.selectList(
                new LambdaQueryWrapper<AiStateTrace>()
                        .eq(AiStateTrace::getSessionId, sessionId)
                        .eq(AiStateTrace::getStatus, "COMPLETED")
        ).stream().map(AiStateTrace::getNodeId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    // ========== Data Access Helpers ==========

    private AiStateGraph loadGraph(String graphCode) {
        AiStateGraph graph = graphMapper.selectOne(
                new LambdaQueryWrapper<AiStateGraph>()
                        .eq(AiStateGraph::getGraphCode, graphCode)
                        .eq(AiStateGraph::getStatus, "ACTIVE")
        );
        if (graph == null) {
            throw new IllegalArgumentException("Graph not found or not active: " + graphCode);
        }
        return graph;
    }

    private List<AiStateNode> loadNodes(Long graphId) {
        return nodeMapper.selectList(
                new LambdaQueryWrapper<AiStateNode>()
                        .eq(AiStateNode::getGraphId, graphId)
                        .orderByAsc(AiStateNode::getSortOrder)
        );
    }

    private AiStateSession loadSession(String runId) {
        AiStateSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AiStateSession>()
                        .eq(AiStateSession::getRunId, runId)
        );
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + runId);
        }
        return session;
    }

    private AiStateTrace createTrace(Long sessionId, AiStateNode node) {
        AiStateTrace trace = new AiStateTrace();
        trace.setSessionId(sessionId);
        trace.setNodeId(node.getNodeId());
        trace.setNodeCode(node.getNodeCode());
        trace.setNodeType(node.getNodeType());
        trace.setStatus("RUNNING");
        trace.setStartedAt(LocalDateTime.now());
        return trace;
    }

    private void failSession(AiStateSession session, String errorMessage) {
        session.setStatus("FAILED");
        session.setErrorMessage(errorMessage);
        session.setCompletedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    // ========== JSON Helpers ==========

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error", e);
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON deserialization error", e);
            return new HashMap<>();
        }
    }

    private Object fromJsonOptional(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ========== Inner Types ==========

    private enum ExecutionAction {
        CONTINUE, INTERRUPT, FAIL
    }
}