package com.goat.cloud.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.common.web.PageQuery;
import com.goat.cloud.module.ai.mapper.AiAgentMapper;
import com.goat.cloud.module.ai.mapper.AiApiSkillMapper;
import com.goat.cloud.module.ai.mapper.AiBillingRecordMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasetMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTableMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTermMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentChunkMapper;
import com.goat.cloud.module.ai.mapper.AiDocumentMapper;
import com.goat.cloud.module.ai.mapper.AiKnowledgeBaseMapper;
import com.goat.cloud.module.ai.mapper.AiMcpToolMapper;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.mapper.AiPromptTemplateMapper;
import com.goat.cloud.module.ai.mapper.AiVectorConfigMapper;
import com.goat.cloud.module.ai.mapper.AiWorkflowMapper;
import com.goat.cloud.module.ai.model.AiCatalogItem;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.request.AiPageQuery;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.model.vo.AiModuleOverviewVO;
import com.goat.cloud.module.ai.model.vo.AiOverviewVO;
import com.goat.cloud.module.ai.runtime.AiRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private static final int DEFAULT_LIST_LIMIT = 200;

    private final AiModelConfigMapper modelConfigMapper;
    private final AiVectorConfigMapper vectorConfigMapper;
    private final AiPromptTemplateMapper promptTemplateMapper;
    private final AiBillingRecordMapper billingRecordMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiDocumentMapper documentMapper;
    private final AiDocumentChunkMapper documentChunkMapper;
    private final AiMcpToolMapper mcpToolMapper;
    private final AiApiSkillMapper apiSkillMapper;
    private final AiChatBiDatasourceMapper chatBiDatasourceMapper;
    private final AiChatBiTableMapper chatBiTableMapper;
    private final AiChatBiDatasetMapper chatBiDatasetMapper;
    private final AiChatBiTermMapper chatBiTermMapper;
    private final AiAgentMapper agentMapper;
    private final AiWorkflowMapper workflowMapper;
    private final AiRuntimeService aiRuntimeService;

    public AiOverviewVO overview() {
        AiOverviewVO overview = new AiOverviewVO();
        overview.setStatus("FRAMEWORK_READY");
        overview.setVectorStore("PostgreSQL + pgvector");
        overview.setCapabilities(List.of(
                "chat mock response",
                "model and vector configuration",
                "prompt and billing statistics",
                "RAG knowledge base, document and chunk management",
                "MCP tools and API Skills registry",
                "ChatBI datasource, table, dataset and term metadata",
                "agent and workflow orchestration metadata"));
        overview.setModules(List.of(
                module("models", "模型配置", "/api/ai/models", "LLM and embedding model registry", modelConfigMapper),
                module("vector-configs", "向量配置", "/api/ai/vector-configs", "pgvector storage and embedding settings", vectorConfigMapper),
                module("prompts", "提示词", "/api/ai/prompts", "system/user prompt templates", promptTemplateMapper),
                module("billing", "账单统计", "/api/ai/billing", "token usage and cost records", billingRecordMapper),
                module("knowledge-bases", "知识库", "/api/ai/knowledge-bases", "RAG knowledge base registry", knowledgeBaseMapper),
                module("documents", "文档管理", "/api/ai/documents", "RAG source document metadata", documentMapper),
                module("chunks", "切片管理", "/api/ai/chunks", "RAG chunks and embedding status", documentChunkMapper),
                module("mcp-tools", "MCP 工具", "/api/ai/mcp-tools", "MCP tool registry", mcpToolMapper),
                module("api-skills", "API Skills", "/api/ai/api-skills", "callable API skills registry", apiSkillMapper),
                module("chatbi-datasources", "问数数据源", "/api/ai/chatbi/datasources", "ChatBI datasource metadata", chatBiDatasourceMapper),
                module("chatbi-tables", "问数数据表", "/api/ai/chatbi/tables", "ChatBI table metadata", chatBiTableMapper),
                module("chatbi-datasets", "问数数据集", "/api/ai/chatbi/datasets", "ChatBI dataset semantic metadata", chatBiDatasetMapper),
                module("chatbi-terms", "问数术语", "/api/ai/chatbi/terms", "ChatBI glossary and metric terms", chatBiTermMapper),
                module("agents", "智能体", "/api/ai/agents", "agent metadata and tool bindings", agentMapper),
                module("workflows", "流程编排", "/api/ai/workflows", "workflow graph metadata", workflowMapper)));
        return overview;
    }

    public AiChatResponse chat(AiChatRequest request) {
        return aiRuntimeService.chat(request);
    }

    public List<AiCatalogItem> list(String section) {
        String normalized = StringUtils.hasText(section) ? section : "all";
        return switch (normalized) {
            case "models" -> List.of(catalog("model-config", "模型配置", "assistant", "READY", "Manage LLM and embedding models."));
            case "vector-configs" -> List.of(catalog("pgvector", "PostgreSQL pgvector", "rag", "READY", "Vector configuration placeholder."));
            case "prompts" -> List.of(catalog("prompt-template", "提示词模板", "assistant", "READY", "Prompt template registry."));
            case "billing" -> List.of(catalog("token-usage", "账单统计", "assistant", "READY", "Token and cost statistics."));
            case "knowledge-bases" -> List.of(catalog("rag-kb", "RAG 知识库", "rag", "READY", "Knowledge base metadata."));
            case "documents" -> List.of(catalog("rag-document", "文档管理", "rag", "READY", "Document metadata."));
            case "chunks" -> List.of(catalog("rag-chunk", "切片管理", "rag", "READY", "Chunk and embedding metadata."));
            case "mcp-tools" -> List.of(catalog("mcp-tool", "MCP 工具", "tools", "READY", "MCP tool registry."));
            case "api-skills" -> List.of(catalog("api-skill", "API Skills", "tools", "READY", "API skill registry."));
            case "chatbi", "chatbi-datasources", "chatbi-tables", "chatbi-datasets", "chatbi-terms" -> List.of(
                    catalog("chatbi-datasource", "问数数据源", "chatbi", "READY", "Datasource metadata."),
                    catalog("chatbi-table", "问数数据表", "chatbi", "READY", "Table metadata."),
                    catalog("chatbi-dataset", "问数数据集", "chatbi", "READY", "Dataset semantic metadata."),
                    catalog("chatbi-term", "问数术语", "chatbi", "READY", "Business glossary."));
            case "agents", "workflows" -> List.of(
                    catalog("agent", "AI 智能体", "agent", "READY", "Agent metadata."),
                    catalog("workflow", "流程编排", "agent", "READY", "Workflow graph metadata."));
            default -> List.of(
                    catalog("assistant", "AI 助手", "assistant", "READY", "Mock chat API and model configuration."),
                    catalog("rag", "RAG 知识库", "rag", "READY", "Knowledge base, documents and chunks."),
                    catalog("chatbi", "AI 问数", "chatbi", "READY", "Datasource, table, dataset and term metadata."),
                    catalog("agent", "智能体与流程", "agent", "READY", "Agent and workflow orchestration metadata."));
        };
    }

    public <T> List<T> list(BaseMapper<T> mapper) {
        try {
            Page<T> page = mapper.selectPage(new Page<>(1, DEFAULT_LIST_LIMIT), buildQuery(mapper, null));
            return page.getRecords();
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return Collections.emptyList();
            }
            throw ex;
        }
    }

    public <T> PageResponse<T> page(BaseMapper<T> mapper, AiPageQuery query) {
        AiPageQuery safeQuery = query == null ? new AiPageQuery() : query;
        try {
            Page<T> page = mapper.selectPage(new Page<>(safeQuery.getPageNum(), safeQuery.getPageSize()), buildQuery(mapper, safeQuery));
            return PageResponse.of(page);
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return new PageResponse<>(Collections.emptyList(), 0, safeQuery.getPageNum(), safeQuery.getPageSize());
            }
            throw ex;
        }
    }

    public <T> T detail(BaseMapper<T> mapper, Long id) {
        try {
            return mapper.selectById(id);
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return null;
            }
            throw ex;
        }
    }

    public <T> void save(BaseMapper<T> mapper, T entity) {
        try {
            if (hasPrimaryKeyValue(entity)) {
                mapper.updateById(entity);
            } else {
                mapper.insert(entity);
            }
        } catch (RuntimeException ex) {
            if (!isMissingTable(ex)) {
                throw ex;
            }
        }
    }

    public <T> void delete(BaseMapper<T> mapper, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        try {
            mapper.deleteByIds(ids);
        } catch (RuntimeException ex) {
            if (!isMissingTable(ex)) {
                throw ex;
            }
        }
    }

    private AiModuleOverviewVO module(String code, String name, String basePath, String description, BaseMapper<?> mapper) {
        return new AiModuleOverviewVO(code, name, basePath, description, count(mapper));
    }

    private AiCatalogItem catalog(String code, String name, String category, String status, String description) {
        return new AiCatalogItem(code, name, category, status, description);
    }

    private Long count(BaseMapper<?> mapper) {
        try {
            return mapper.selectCount(null);
        } catch (RuntimeException ex) {
            if (isMissingTable(ex)) {
                return 0L;
            }
            throw ex;
        }
    }

    private <T> QueryWrapper<T> buildQuery(BaseMapper<T> mapper, AiPageQuery query) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        TableInfo tableInfo = resolveTableInfo(mapper);
        if (tableInfo != null && query != null) {
            if (StringUtils.hasText(query.getStatus()) && hasColumn(tableInfo, "status")) {
                wrapper.eq("status", query.getStatus());
            }
            if (StringUtils.hasText(query.getKeyword())) {
                List<String> keywordColumns = tableInfo.getFieldList().stream()
                        .filter(field -> String.class.equals(field.getPropertyType()))
                        .map(TableFieldInfo::getColumn)
                        .filter(column -> !List.of(
                                "status", "remark", "metadata", "graph_json", "embedding_vector",
                                "retrieval_config", "indexing_config", "access_control",
                                "variables_schema", "auth_config", "timeout_config", "retry_config",
                                "rate_limit_config", "price_config", "edges_json"
                        ).contains(column))
                        .toList();
                if (!keywordColumns.isEmpty()) {
                    String keyword = query.getKeyword().trim();
                    wrapper.and(nested -> {
                        for (int i = 0; i < keywordColumns.size(); i++) {
                            if (i > 0) {
                                nested.or();
                            }
                            nested.like(keywordColumns.get(i), keyword);
                        }
                    });
                }
            }
        }
        return wrapper.orderByDesc("create_time");
    }

    private <T> TableInfo resolveTableInfo(BaseMapper<T> mapper) {
        for (Class<?> mapperInterface : mapper.getClass().getInterfaces()) {
            TableInfo tableInfo = resolveTableInfo(mapperInterface);
            if (tableInfo != null) {
                return tableInfo;
            }
        }
        return null;
    }

    private TableInfo resolveTableInfo(Class<?> type) {
        for (java.lang.reflect.Type genericInterface : type.getGenericInterfaces()) {
            if (genericInterface instanceof java.lang.reflect.ParameterizedType parameterizedType
                    && parameterizedType.getRawType() instanceof Class<?> rawType
                    && BaseMapper.class.isAssignableFrom(rawType)
                    && parameterizedType.getActualTypeArguments().length > 0
                    && parameterizedType.getActualTypeArguments()[0] instanceof Class<?> entityClass) {
                return TableInfoHelper.getTableInfo(entityClass);
            }
            if (genericInterface instanceof Class<?> interfaceClass) {
                TableInfo tableInfo = resolveTableInfo(interfaceClass);
                if (tableInfo != null) {
                    return tableInfo;
                }
            }
        }
        return null;
    }

    private boolean hasColumn(TableInfo tableInfo, String column) {
        return tableInfo.getFieldList().stream().anyMatch(field -> column.equals(field.getColumn()));
    }

    private boolean hasPrimaryKeyValue(Object entity) {
        if (entity == null) {
            return false;
        }
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
        if (tableInfo == null || !StringUtils.hasText(tableInfo.getKeyProperty())) {
            return false;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        Object value = wrapper.getPropertyValue(tableInfo.getKeyProperty());
        return value != null;
    }

    private boolean isMissingTable(RuntimeException ex) {
        Map<String, Boolean> markers = new LinkedHashMap<>();
        for (Throwable current = ex; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            String lower = message.toLowerCase(Locale.ROOT);
            markers.put("relation", lower.contains("relation"));
            markers.put("table", lower.contains("table"));
            markers.put("doesNotExist", lower.contains("does not exist") || lower.contains("doesn't exist"));
            if ((Boolean.TRUE.equals(markers.get("relation")) || Boolean.TRUE.equals(markers.get("table")))
                    && Boolean.TRUE.equals(markers.get("doesNotExist"))) {
                return true;
            }
        }
        return false;
    }
}
