package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.runtime.AiChatBiService;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ChatBI 节点执行器
 * <p>
 * 在工作流中执行 ChatBI 语义查询，生成候选 SQL 草案
 * <p>
 * 节点配置示例：
 * {
 *   "datasetId": 1,
 *   "datasourceId": 2,
 *   "limit": 100
 * }
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBiNodeExecutor implements NodeExecutor {

    private final AiChatBiService chatBiService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "CHATBI";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            if (nodeConfig != null && !nodeConfig.isBlank()) {
                config = objectMapper.readValue(nodeConfig, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            }

            // Resolve datasetId from config, fallback to context
            Long datasetId = toLong(config.get("datasetId"));
            if (datasetId == null) {
                datasetId = toLong(context.get("datasetId"));
            }

            // Resolve datasourceId from config, fallback to context
            Long datasourceId = toLong(config.get("datasourceId"));
            if (datasourceId == null) {
                datasourceId = toLong(context.get("datasourceId"));
            }

            // Resolve limit from config, fallback to context, default 100
            Integer limit = toInteger(config.get("limit"));
            if (limit == null) {
                limit = toInteger(context.get("limit"));
            }
            if (limit == null) {
                limit = 100;
            }

            // Resolve question from context userMessage
            String question = asString(context.get("userMessage"));
            if (question.isBlank()) {
                return NodeResult.fail("ChatBI requires a question (userMessage in context)");
            }

            ChatBiAskRequest chatBiRequest = new ChatBiAskRequest();
            chatBiRequest.setQuestion(question);
            chatBiRequest.setDatasetId(datasetId);
            chatBiRequest.setDatasourceId(datasourceId);
            chatBiRequest.setLimit(limit);

            ChatBiAskResponse chatBiResponse = chatBiService.askChatBi(chatBiRequest);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("candidateSql", chatBiResponse.getCandidateSql());
            output.put("executionPolicy", chatBiResponse.getExecutionPolicy());
            output.put("warnings", chatBiResponse.getSafetyWarnings());
            output.put("explanation", chatBiResponse.getExplanation());

            context.put("chatBiResult", output);

            return NodeResult.ok(objectMapper.writeValueAsString(output));

        } catch (Exception e) {
            log.error("ChatBI node execution error", e);
            return NodeResult.fail("ChatBI failed: " + e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }
}