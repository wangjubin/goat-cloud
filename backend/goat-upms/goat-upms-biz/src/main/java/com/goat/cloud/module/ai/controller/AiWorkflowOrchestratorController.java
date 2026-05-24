package com.goat.cloud.module.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.ai.entity.AiStateGraph;
import com.goat.cloud.module.ai.entity.AiStateNode;
import com.goat.cloud.module.ai.mapper.AiStateGraphMapper;
import com.goat.cloud.module.ai.mapper.AiStateNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流编排控制器
 * <p>
 * 提供可视化工作流设计器所需的所有 API：
 * - 图定义 CRUD
 * - 节点 CRUD（含边信息）
 * - 图的发布/下线
 * - 图结构验证
 * @author wangjubin
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chatbi/workflow")
@RequiredArgsConstructor
public class AiWorkflowOrchestratorController {

    private final AiStateGraphMapper graphMapper;
    private final AiStateNodeMapper nodeMapper;
    private final ObjectMapper objectMapper;

    /**
     * 获取完整的工作流定义（图 + 节点 + 边）
     */
    @GetMapping("/{graphId}/definition")
    public ApiResponse<Map<String, Object>> getWorkflowDefinition(@PathVariable Long graphId) {
        AiStateGraph graph = graphMapper.selectById(graphId);
        if (graph == null) {
            return ApiResponse.fail(4001, "Graph not found");
        }

        List<AiStateNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<AiStateNode>()
                        .eq(AiStateNode::getGraphId, graphId)
                        .orderByAsc(AiStateNode::getSortOrder)
        );

        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("graph", graph);
        definition.put("nodes", nodes);
        definition.put("edges", buildEdgeList(nodes));

        return ApiResponse.success(definition);
    }

    /**
     * 保存完整的工作流定义（一次性保存图 + 节点）
     */
    @PostMapping("/save-definition")
    public ApiResponse<Void> saveWorkflowDefinition(@RequestBody WorkflowDefinitionRequest request) {
        // Save graph
        AiStateGraph graph;
        if (request.getGraphId() != null) {
            graph = graphMapper.selectById(request.getGraphId());
            if (graph == null) {
                return ApiResponse.fail(4001, "Graph not found");
            }
            updateGraphFromRequest(graph, request);
            graphMapper.updateById(graph);
        } else {
            graph = new AiStateGraph();
            updateGraphFromRequest(graph, request);
            graph.setStatus("DRAFT");
            graphMapper.insert(graph);
        }

        // Save nodes (delete old + insert new)
        if (request.getNodes() != null) {
            nodeMapper.delete(
                    new LambdaQueryWrapper<AiStateNode>()
                            .eq(AiStateNode::getGraphId, graph.getGraphId())
            );
            for (NodeDefinition nodeDef : request.getNodes()) {
                AiStateNode node = new AiStateNode();
                node.setGraphId(graph.getGraphId());
                node.setNodeCode(nodeDef.getNodeCode());
                node.setNodeName(nodeDef.getNodeName());
                node.setNodeType(nodeDef.getNodeType());
                node.setConfigJson(nodeDef.getConfigJson());
                node.setSortOrder(nodeDef.getSortOrder() != null ? nodeDef.getSortOrder() : 0);
                node.setTimeoutMs(nodeDef.getTimeoutMs() != null ? nodeDef.getTimeoutMs() : 30000);

                // Build edges JSON from incoming/outgoing
                if (nodeDef.getOutgoing() != null || nodeDef.getIncoming() != null) {
                    try {
                        Map<String, Object> edges = new LinkedHashMap<>();
                        if (nodeDef.getOutgoing() != null) edges.put("outgoing", nodeDef.getOutgoing());
                        if (nodeDef.getIncoming() != null) edges.put("incoming", nodeDef.getIncoming());
                        node.setEdgesJson(objectMapper.writeValueAsString(edges));
                    } catch (Exception e) {
                        log.warn("Failed to serialize edges", e);
                    }
                }

                nodeMapper.insert(node);
            }
        }

        return ApiResponse.success();
    }

    /**
     * 发布工作流（DRAFT -> ACTIVE）
     */
    @PostMapping("/{graphId}/publish")
    public ApiResponse<Void> publishWorkflow(@PathVariable Long graphId) {
        AiStateGraph graph = graphMapper.selectById(graphId);
        if (graph == null) {
            return ApiResponse.fail(4001, "Graph not found");
        }

        // Validate: must have at least one node
        long nodeCount = nodeMapper.selectCount(
                new LambdaQueryWrapper<AiStateNode>()
                        .eq(AiStateNode::getGraphId, graphId)
        );
        if (nodeCount == 0) {
            return ApiResponse.fail(4001, "Cannot publish: graph has no nodes");
        }

        graph.setStatus("ACTIVE");
        graphMapper.updateById(graph);
        return ApiResponse.success();
    }

    /**
     * 下线工作流（ACTIVE -> DRAFT）
     */
    @PostMapping("/{graphId}/unpublish")
    public ApiResponse<Void> unpublishWorkflow(@PathVariable Long graphId) {
        AiStateGraph graph = graphMapper.selectById(graphId);
        if (graph == null) {
            return ApiResponse.fail(4001, "Graph not found");
        }
        graph.setStatus("DRAFT");
        graphMapper.updateById(graph);
        return ApiResponse.success();
    }

    /**
     * 验证工作流结构
     */
    @PostMapping("/{graphId}/validate")
    public ApiResponse<Map<String, Object>> validateWorkflow(@PathVariable Long graphId) {
        List<AiStateNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<AiStateNode>()
                        .eq(AiStateNode::getGraphId, graphId)
        );

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (nodes.isEmpty()) {
            errors.add("Graph has no nodes");
        }

        // Check for START node in DAG mode
        boolean hasStart = nodes.stream().anyMatch(n -> "START".equals(n.getNodeType()));
        boolean hasEnd = nodes.stream().anyMatch(n -> "END".equals(n.getNodeType()));

        Set<String> nodeCodes = nodes.stream().map(AiStateNode::getNodeCode).collect(Collectors.toSet());

        // Check for orphan nodes (no incoming edges except START)
        for (AiStateNode node : nodes) {
            if ("START".equals(node.getNodeType())) continue;
            boolean hasIncoming = false;
            for (AiStateNode other : nodes) {
                if (other.getEdgesJson() != null && other.getEdgesJson().contains(node.getNodeCode())) {
                    hasIncoming = true;
                    break;
                }
            }
            if (!hasIncoming && node.getSortOrder() != null && node.getSortOrder() > 1) {
                warnings.add("Node '" + node.getNodeCode() + "' has no incoming edges");
            }
        }

        // Check for unknown node types
        Set<String> validTypes = Set.of(
                "START", "END", "GATEWAY", "INTENT_RECOGNITION", "SCHEMA_RECALL",
                "NL2SQL", "SQL_EXECUTION", "HUMAN_FEEDBACK", "PYTHON_EXECUTION",
                "REPORT_GENERATION"
        );
        for (AiStateNode node : nodes) {
            if (!validTypes.contains(node.getNodeType())) {
                warnings.add("Node '" + node.getNodeCode() + "' has unknown type: " + node.getNodeType());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("nodeCount", nodes.size());
        result.put("hasStartNode", hasStart);
        result.put("hasEndNode", hasEnd);
        return ApiResponse.success(result);
    }

    /**
     * 列出所有工作流图
     */
    @GetMapping("/list")
    public ApiResponse<List<AiStateGraph>> listWorkflows(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String graphType) {
        LambdaQueryWrapper<AiStateGraph> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(AiStateGraph::getStatus, status);
        if (graphType != null) wrapper.eq(AiStateGraph::getGraphType, graphType);
        wrapper.orderByDesc(AiStateGraph::getCreateTime);
        return ApiResponse.success(graphMapper.selectList(wrapper));
    }

    // ========== Helpers ==========

    private void updateGraphFromRequest(AiStateGraph graph, WorkflowDefinitionRequest request) {
        if (request.getGraphCode() != null) graph.setGraphCode(request.getGraphCode());
        if (request.getGraphName() != null) graph.setGraphName(request.getGraphName());
        if (request.getDescription() != null) graph.setDescription(request.getDescription());
        if (request.getGraphType() != null) graph.setGraphType(request.getGraphType());
        if (request.getDefinitionJson() != null) graph.setDefinitionJson(request.getDefinitionJson());
        if (request.getConfigJson() != null) graph.setConfigJson(request.getConfigJson());
    }

    private List<Map<String, Object>> buildEdgeList(List<AiStateNode> nodes) {
        List<Map<String, Object>> edges = new ArrayList<>();
        for (AiStateNode node : nodes) {
            if (node.getEdgesJson() != null && !node.getEdgesJson().isBlank()) {
                try {
                    Map<String, Object> edgeDef = objectMapper.readValue(node.getEdgesJson(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    Object outgoing = edgeDef.get("outgoing");
                    if (outgoing instanceof List<?> list) {
                        for (Object item : list) {
                            Map<String, Object> edge = new LinkedHashMap<>();
                            edge.put("from", node.getNodeCode());
                            if (item instanceof Map<?, ?> m) {
                                edge.put("to", m.get("to"));
                                edge.put("condition", m.get("condition"));
                            } else {
                                edge.put("to", String.valueOf(item));
                            }
                            edges.add(edge);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse edges for node {}", node.getNodeCode());
                }
            }
        }
        return edges;
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class WorkflowDefinitionRequest {
        private Long graphId;
        private String graphCode;
        private String graphName;
        private String description;
        private String graphType;
        private String definitionJson;
        private String configJson;
        private List<NodeDefinition> nodes;
    }

    @lombok.Data
    public static class NodeDefinition {
        private String nodeCode;
        private String nodeName;
        private String nodeType;
        private String configJson;
        private Integer sortOrder;
        private Integer timeoutMs;
        private List<Map<String, Object>> outgoing;
        private List<String> incoming;
    }
}