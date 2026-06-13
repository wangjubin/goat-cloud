package com.goat.cloud.module.ai.runtime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiBillingRecord;
import com.goat.cloud.module.ai.entity.AiConversationRecord;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiBillingRecordMapper;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.memory.ShortTermMessage;
import com.goat.cloud.module.ai.memory.ShortTermMemoryStore;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatMessage;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.model.vo.AiTokenUsageVO;
import com.goat.cloud.module.ai.runtime.model.RagSearchHit;
import com.goat.cloud.module.ai.runtime.model.RagSearchRequest;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import com.goat.cloud.module.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int DEFAULT_RAG_TOP_K = 5;
    private static final int DEFAULT_SHORT_TERM_WINDOW = 20;

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final AiModelConfigMapper modelConfigMapper;
    private final AiBillingRecordMapper billingRecordMapper;
    private final AiRagSearchService ragSearchService;
    private final AiConversationService conversationService;
    private final ShortTermMemoryStore shortTermMemoryStore;

    public AiChatResponse chat(AiChatRequest request) {
        AiChatRequest safeRequest = request == null ? new AiChatRequest() : request;
        Map<String, Object> options = safeOptions(safeRequest.getOptions());
        String conversationId = StringUtils.hasText(safeRequest.getConversationId())
                ? safeRequest.getConversationId()
                : UUID.randomUUID().toString();
        String userText = StringUtils.hasText(safeRequest.getMessage())
                ? safeRequest.getMessage()
                : latestUserText(safeRequest.getMessages());

        // 加载短期记忆历史
        int windowSize = AiRuntimeHelper.toInteger(options.get("shortTermWindow"), DEFAULT_SHORT_TERM_WINDOW);
        List<ShortTermMessage> historyMessages = shortTermMemoryStore.loadHistory(conversationId, windowSize);

        // 保存用户消息到短期记忆和持久化
        if (StringUtils.hasText(userText)) {
            shortTermMemoryStore.append(conversationId, "user", userText, windowSize);
            try {
                Long agentId = AiRuntimeHelper.toLong(options.get("agentId"));
                Long userId = AiRuntimeHelper.toLong(options.get("userId"));
                if (agentId != null && userId != null) {
                    conversationService.getOrCreateConversation(conversationId, agentId, userId, userText);
                    conversationService.saveMessage(conversationId, agentId, userId, "user", userText, null);
                }
            } catch (Exception ex) {
                // 对话持久化失败不阻塞主流程
            }
        }

        AiModelConfig model = resolveModel(safeRequest);
        String provider = firstText(safeRequest.getProvider(), model == null ? null : model.getProvider(), "local-runtime");
        String modelCode = firstText(safeRequest.getModelCode(), model == null ? null : model.getModelCode(), "local-rules");
        String systemPrompt = firstText(safeRequest.getSystemPrompt(), "You are Goat Cloud enterprise AI assistant. Answer with concise, grounded reasoning.");

        RagSearchResponse rag = null;
        if (StringUtils.hasText(userText) && isRagEnabled(options)) {
            RagSearchRequest ragRequest = new RagSearchRequest();
            ragRequest.setQuery(userText);
            ragRequest.setTopK(AiRuntimeHelper.toInteger(options.get("topK"), DEFAULT_RAG_TOP_K));
            ragRequest.setIncludeContent(Boolean.TRUE);
            ragRequest.setKnowledgeBaseId(AiRuntimeHelper.toLong(options.get("knowledgeBaseId")));
            ragRequest.setKnowledgeBaseIds(toLongList(options.get("knowledgeBaseIds")));
            ragRequest.setDocumentId(AiRuntimeHelper.toLong(options.get("documentId")));
            rag = ragSearchService.search(ragRequest);
        }

        String ragContext = ragSearchService.buildRagContext(rag);
        String apiKey = resolveApiKey(model == null ? null : model.getApiKeyRef());
        boolean canCallProvider = model != null
                && StringUtils.hasText(model.getEndpoint())
                && StringUtils.hasText(apiKey);
        ProviderResult providerResult = canCallProvider
                ? callOpenAiCompatible(model, apiKey, systemPrompt, ragContext, userText, historyMessages, safeRequest)
                : ProviderResult.skipped(missingProviderReason(model, apiKey));

        String answer = providerResult.success()
                ? providerResult.content()
                : localAnswer(userText, rag, providerResult.error());
        AiTokenUsageVO usage = providerResult.usage() == null
                ? estimateUsage(systemPrompt + "\n" + ragContext + "\n" + userText, answer)
                : providerResult.usage();

        // 保存助手回复到短期记忆和持久化
        if (StringUtils.hasText(answer)) {
            shortTermMemoryStore.append(conversationId, "assistant", answer, windowSize);
            try {
                Long agentId = AiRuntimeHelper.toLong(options.get("agentId"));
                Long userId = AiRuntimeHelper.toLong(options.get("userId"));
                if (agentId != null && userId != null) {
                    conversationService.saveMessage(conversationId, agentId, userId, "assistant", answer, null);
                }
            } catch (Exception ex) {
                // 对话持久化失败不阻塞主流程
            }
        }

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

        recordBilling(conversationId, provider, modelCode, firstText(AiRuntimeHelper.asString(options.get("bizType")), "CHAT"), usage, response.getFinishReason());
        return response;
    }

    private ProviderResult callOpenAiCompatible(AiModelConfig model, String apiKey, String systemPrompt,
                                                String ragContext, String userText,
                                                List<ShortTermMessage> historyMessages, AiChatRequest request) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            if (StringUtils.hasText(ragContext)) {
                messages.add(Map.of("role", "system", "content", "Use these retrieved citations when relevant:\n" + ragContext));
            }
            // 注入历史消息（多轮对话上下文）
            if (historyMessages != null && !historyMessages.isEmpty()) {
                for (ShortTermMessage history : historyMessages) {
                    if (StringUtils.hasText(history.getRole()) && StringUtils.hasText(history.getContent())) {
                        messages.add(Map.of("role", history.getRole(), "content", history.getContent()));
                    }
                }
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
        answer.append("本地规则回答：当前未完成外部大模型调用，系统已基于 Goat Cloud AI 元数据和 RAG 检索生成可解释结果。");
        if (StringUtils.hasText(fallbackReason)) {
            answer.append("降级原因：").append(fallbackReason).append("。");
        }
        if (rag != null && rag.getHits() != null && !rag.getHits().isEmpty()) {
            answer.append("针对问题\"").append(firstText(userText, "未提供问题")).append("\"，检索到 ")
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

    private AiModelConfig resolveModel(AiChatRequest request) {
        if (request.getModelId() != null) {
            AiModelConfig byId = safeSelectById(modelConfigMapper, request.getModelId());
            if (byId != null) return byId;
        }
        if (StringUtils.hasText(request.getModelCode())) {
            AiModelConfig byCode = safeSelectOne(modelConfigMapper, new QueryWrapper<AiModelConfig>()
                    .eq("model_code", request.getModelCode())
                    .eq("status", "ENABLED")
                    .last("limit 1"));
            if (byCode != null) return byCode;
        }
        AiModelConfig defaultChat = safeSelectOne(modelConfigMapper, new QueryWrapper<AiModelConfig>()
                .eq("model_type", "CHAT")
                .eq("default_model", true)
                .eq("status", "ENABLED")
                .orderByAsc("sort_order")
                .last("limit 1"));
        if (defaultChat != null) return defaultChat;
        return safeSelectOne(modelConfigMapper, new QueryWrapper<AiModelConfig>()
                .eq("status", "ENABLED")
                .orderByAsc("sort_order")
                .last("limit 1"));
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
            if (!AiRuntimeHelper.isMissingTable(ex)) {
                throw ex;
            }
        }
    }

    private String resolveApiKey(String apiKeyRef) {
        if (!StringUtils.hasText(apiKeyRef)) return null;
        String ref = apiKeyRef.trim();
        if (ref.startsWith("ENV:")) return lookupSecret(ref.substring(4));
        if (ref.startsWith("PROP:")) return environment.getProperty(ref.substring(5));
        if (ref.startsWith("${") && ref.endsWith("}")) return lookupSecret(ref.substring(2, ref.length() - 1));
        if (ref.startsWith("VALUE:")) return ref.substring(6);
        return lookupSecret(ref);
    }

    private String lookupSecret(String name) {
        if (!StringUtils.hasText(name)) return null;
        String value = environment.getProperty(name);
        if (!StringUtils.hasText(value)) value = System.getenv(name);
        return StringUtils.hasText(value) ? value : null;
    }

    private String completionUrl(String endpoint) {
        String trimmed = endpoint.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.endsWith("/chat/completions")) return trimmed;
        if (trimmed.endsWith("/v1")) return trimmed + "/chat/completions";
        return trimmed + "/v1/chat/completions";
    }

    private String missingProviderReason(AiModelConfig model, String apiKey) {
        if (model == null) return "no enabled chat model configuration was found";
        if (!StringUtils.hasText(model.getEndpoint())) return "model endpoint is not configured";
        if (!StringUtils.hasText(apiKey)) return "apiKeyRef did not resolve to an environment/property value";
        return "provider call skipped";
    }

    private boolean isRagEnabled(Map<String, Object> options) {
        Object rag = options.get("rag");
        Object useRag = options.get("useRag");
        return !Boolean.FALSE.equals(AiRuntimeHelper.toBoolean(rag)) && !Boolean.FALSE.equals(AiRuntimeHelper.toBoolean(useRag));
    }

    private AiTokenUsageVO estimateUsage(String prompt, String answer) {
        int promptTokens = estimateTokens(prompt);
        int completionTokens = estimateTokens(answer);
        return new AiTokenUsageVO(promptTokens, completionTokens, promptTokens + completionTokens);
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return Math.max(1, text.length() / 4);
    }

    private String latestUserText(List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessage message = messages.get(i);
            if (message != null && "user".equalsIgnoreCase(message.getRole())) return message.getContent();
        }
        AiChatMessage last = messages.get(messages.size() - 1);
        return last == null ? "" : last.getContent();
    }

    private Map<String, Object> safeOptions(Map<String, Object> options) {
        return options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    private List<Long> toLongList(Object value) {
        if (value == null) return List.of();
        if (value instanceof java.util.Collection<?> collection) {
            return collection.stream().map(AiRuntimeHelper::toLong).filter(java.util.Objects::nonNull).toList();
        }
        return parseLongList(String.valueOf(value));
    }

    private List<Long> parseLongList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("[,，\\s]+")) {
            if (StringUtils.hasText(token)) tokens.add(token.trim());
        }
        return tokens.stream().map(AiRuntimeHelper::toLong).filter(java.util.Objects::nonNull).toList();
    }

    static String firstText(String... values) {
        return AiRuntimeHelper.firstText(values);
    }

    private <T> T safeSelectById(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, java.io.Serializable id) {
        try { return mapper.selectById(id); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return null; throw ex; }
    }

    private <T> T safeSelectOne(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, QueryWrapper<T> wrapper) {
        try { return mapper.selectOne(wrapper); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return null; throw ex; }
    }

    record ProviderResult(String content, String finishReason, AiTokenUsageVO usage, String error, boolean success) {
        static ProviderResult ok(String content, String finishReason, AiTokenUsageVO usage) {
            return new ProviderResult(content, finishReason, usage, null, true);
        }
        static ProviderResult failed(String error) {
            return new ProviderResult(null, null, null, error, false);
        }
        static ProviderResult skipped(String reason) { return failed(reason); }
    }
}