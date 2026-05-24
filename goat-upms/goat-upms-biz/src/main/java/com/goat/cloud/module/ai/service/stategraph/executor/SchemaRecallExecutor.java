package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiChatBiDatasource;
import com.goat.cloud.module.ai.entity.AiChatBiTable;
import com.goat.cloud.module.ai.entity.AiSchemaCache;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTableMapper;
import com.goat.cloud.module.ai.mapper.AiSchemaCacheMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Schema 召回节点执行器
 * <p>
 * 根据用户问题和意图，召回相关的数据库表结构信息
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaRecallExecutor implements NodeExecutor {

    private final AiChatBiDatasourceMapper datasourceMapper;
    private final AiChatBiTableMapper tableMapper;
    private final AiSchemaCacheMapper schemaCacheMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "SCHEMA_RECALL";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            Long datasourceId = toLong(context.get("datasourceId"));
            String question = (String) context.getOrDefault("question", "");

            // 1. 检查 schema 缓存
            AiSchemaCache cached = findSchemaCache(datasourceId, question);
            if (cached != null && cached.getExpiresAt() != null && cached.getExpiresAt().isAfter(LocalDateTime.now())) {
                cached.setHitCount(cached.getHitCount() + 1);
                cached.setLastHitAt(LocalDateTime.now());
                schemaCacheMapper.updateById(cached);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("source", "cache");
                result.put("schemaSnapshot", objectMapper.readValue(cached.getSchemaSnapshot(), Map.class));
                context.put("schemaRecallResult", result);
                return NodeResult.ok(objectMapper.writeValueAsString(result));
            }

            // 2. 加载数据源和表结构
            AiChatBiDatasource datasource = datasourceId != null ? datasourceMapper.selectById(datasourceId) : null;
            List<AiChatBiTable> tables = tableMapper.selectList(
                    new LambdaQueryWrapper<AiChatBiTable>()
                            .eq(datasourceId != null, AiChatBiTable::getDatasourceId, datasourceId)
                            .eq(AiChatBiTable::getStatus, "ENABLED")
                            .orderByAsc(AiChatBiTable::getTableId)
                            .last("limit 20")
            );

            // 3. 基于问题关键词过滤相关表
            List<Map<String, Object>> relevantTables = filterRelevantTables(tables, question);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("source", "metadata");
            result.put("datasourceId", datasourceId);
            result.put("datasourceName", datasource != null ? datasource.getDatasourceName() : null);
            result.put("tables", relevantTables);
            result.put("tableCount", relevantTables.size());

            // 4. 写入缓存
            saveSchemaCache(datasourceId, question, result);

            context.put("schemaRecallResult", result);
            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Schema recall error", e);
            return NodeResult.fail("Schema recall failed: " + e.getMessage());
        }
    }

    private AiSchemaCache findSchemaCache(Long datasourceId, String question) {
        if (datasourceId == null) return null;
        String cacheKey = generateCacheKey(question);
        return schemaCacheMapper.selectOne(
                new LambdaQueryWrapper<AiSchemaCache>()
                        .eq(AiSchemaCache::getDatasourceId, datasourceId)
                        .eq(AiSchemaCache::getCacheKey, cacheKey)
        );
    }

    private List<Map<String, Object>> filterRelevantTables(List<AiChatBiTable> tables, String question) {
        List<Map<String, Object>> result = new ArrayList<>();
        String lowerQ = question.toLowerCase();

        for (AiChatBiTable table : tables) {
            Map<String, Object> tableInfo = new LinkedHashMap<>();
            tableInfo.put("tableId", table.getTableId());
            tableInfo.put("schemaName", table.getSchemaName());
            tableInfo.put("tableName", table.getTableName());
            tableInfo.put("tableComment", table.getTableComment());
            tableInfo.put("columnsJson", table.getColumnsJson());

            // 计算相关性分数
            double score = 0;
            if (table.getTableName() != null && lowerQ.contains(table.getTableName().toLowerCase())) score += 5;
            if (table.getTableComment() != null && lowerQ.contains(table.getTableComment().toLowerCase())) score += 3;
            tableInfo.put("relevanceScore", score);
            result.add(tableInfo);
        }

        // 按相关性排序，保留前 10
        result.sort((a, b) -> Double.compare((double) b.get("relevanceScore"), (double) a.get("relevanceScore")));
        if (result.size() > 10) result = result.subList(0, 10);

        return result;
    }

    private String generateCacheKey(String question) {
        return "schema:" + (question != null ? question.hashCode() : 0);
    }

    private void saveSchemaCache(Long datasourceId, String question, Map<String, Object> result) {
        if (datasourceId == null) return;
        try {
            AiSchemaCache cache = new AiSchemaCache();
            cache.setDatasourceId(datasourceId);
            cache.setCacheKey(generateCacheKey(question));
            cache.setSchemaSnapshot(objectMapper.writeValueAsString(result));
            cache.setHitCount(0);
            cache.setExpiresAt(LocalDateTime.now().plusHours(24));
            schemaCacheMapper.insert(cache);
        } catch (Exception e) {
            log.warn("Failed to save schema cache", e);
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}