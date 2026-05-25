package com.goat.cloud.module.ai.runtime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiChatBiDatasource;
import com.goat.cloud.module.ai.entity.AiChatBiDataset;
import com.goat.cloud.module.ai.entity.AiChatBiTable;
import com.goat.cloud.module.ai.entity.AiChatBiTerm;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasetMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTableMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTermMapper;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse;
import com.goat.cloud.module.ai.runtime.model.ChatBiTableSnapshot;
import com.goat.cloud.module.ai.runtime.model.ChatBiTermMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiChatBiService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern MUTATION_SQL = Pattern.compile("(?i)\\b(insert|update|delete|drop|truncate|alter|grant|revoke|merge|call|execute)\\b");

    private final ObjectMapper objectMapper;
    private final AiChatBiDatasourceMapper chatBiDatasourceMapper;
    private final AiChatBiDatasetMapper chatBiDatasetMapper;
    private final AiChatBiTableMapper chatBiTableMapper;
    private final AiChatBiTermMapper chatBiTermMapper;
    private final AiRagSearchService ragSearchService;

    public ChatBiAskResponse askChatBi(ChatBiAskRequest request) {
        ChatBiAskRequest safeRequest = request == null ? new ChatBiAskRequest() : request;
        String question = AiRuntimeHelper.normalizeText(safeRequest.getQuestion());
        AiChatBiDataset dataset = resolveDataset(safeRequest);
        Long datasourceId = firstLong(safeRequest.getDatasourceId(), dataset == null ? null : dataset.getDatasourceId());
        AiChatBiDatasource datasource = datasourceId == null ? null : safeSelectById(chatBiDatasourceMapper, datasourceId);
        List<AiChatBiTable> tables = resolveTables(dataset, datasourceId);
        List<AiChatBiTerm> terms = resolveTerms(dataset);
        List<ChatBiTermMatch> matchedTerms = terms.stream()
                .map(term -> toTermMatch(term, question))
                .filter(match -> match.getScore() > 0)
                .sorted(java.util.Comparator.comparing(ChatBiTermMatch::getScore, java.util.Comparator.reverseOrder()))
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
        response.setCreatedAt(java.time.LocalDateTime.now());
        return response;
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
        int limit = AiRuntimeHelper.clamp(request.getLimit(), 100, 1, 500);
        String lowerQuestion = AiRuntimeHelper.lower(question);
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
        String haystack = AiRuntimeHelper.lower(String.join(" ",
                firstText(term.getTermCode(), ""),
                firstText(term.getTermName(), ""),
                firstText(term.getSynonyms(), ""),
                firstText(term.getDefinition(), "")));
        for (String termText : ragSearchService.searchTerms(question)) {
            if (haystack.contains(AiRuntimeHelper.lower(termText))) {
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

    private AiChatBiDataset resolveDataset(ChatBiAskRequest request) {
        if (request.getDatasetId() != null) {
            AiChatBiDataset dataset = safeSelectById(chatBiDatasetMapper, request.getDatasetId());
            if (dataset != null) return dataset;
        }
        if (StringUtils.hasText(request.getDatasetCode())) {
            AiChatBiDataset dataset = safeSelectOne(chatBiDatasetMapper, new QueryWrapper<AiChatBiDataset>()
                    .eq("dataset_code", request.getDatasetCode())
                    .eq("status", "ENABLED")
                    .last("limit 1"));
            if (dataset != null) return dataset;
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

    private String selectColumns(AiChatBiTable table) {
        List<Map<String, Object>> columns = parseColumns(table.getColumnsJson());
        List<String> names = columns.stream()
                .map(column -> AiRuntimeHelper.asString(column.get("name")))
                .filter(this::isSafeIdentifier)
                .limit(8)
                .toList();
        if (names.isEmpty()) return "*";
        return String.join(", ", names);
    }

    private String tableReference(AiChatBiTable table) {
        if (table == null || !isSafeIdentifier(table.getTableName())) return null;
        if (StringUtils.hasText(table.getSchemaName())) {
            if (!isSafeIdentifier(table.getSchemaName())) return null;
            return table.getSchemaName() + "." + table.getTableName();
        }
        return table.getTableName();
    }

    private boolean isSafeReadExpression(String expression) {
        if (!StringUtils.hasText(expression) || expression.contains(";") || MUTATION_SQL.matcher(expression).find()) return false;
        return expression.matches("[A-Za-z0-9_.*(),\\s+\\-/=<>]+");
    }

    private String safeFilter(String filter, List<String> warnings) {
        if (!StringUtils.hasText(filter)) return null;
        if (filter.contains(";") || MUTATION_SQL.matcher(filter).find()) {
            warnings.add("Dataset default filter contains unsafe SQL keywords and was ignored.");
            return null;
        }
        return filter;
    }

    private String safeAlias(String alias) {
        String normalized = alias == null ? "metric_value" : alias.replaceAll("[^A-Za-z0-9_]", "_");
        if (!SAFE_IDENTIFIER.matcher(normalized).matches()) return "metric_value";
        return normalized;
    }

    private boolean isSafeIdentifier(String text) {
        return StringUtils.hasText(text) && SAFE_IDENTIFIER.matcher(text).matches();
    }

    private List<Map<String, Object>> parseColumns(String columnsJson) {
        if (!StringUtils.hasText(columnsJson)) return List.of();
        try {
            JsonNode node = objectMapper.readTree(columnsJson);
            JsonNode columnsNode = node.isArray() ? node : node.path("columns");
            if (!columnsNode.isArray()) return List.of(Map.of("raw", columnsJson));
            return objectMapper.convertValue(columnsNode, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return List.of(Map.of("raw", columnsJson));
        }
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

    private List<Long> parseLongList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("[,，\\s]+")) {
            if (StringUtils.hasText(token)) tokens.add(token.trim());
        }
        return tokens.stream().map(AiRuntimeHelper::toLong).filter(java.util.Objects::nonNull).toList();
    }

    private Long firstLong(Long first, Long second) { return first == null ? second : first; }

    static String firstText(String... values) { return AiRuntimeHelper.firstText(values); }

    private <T> T safeSelectById(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, java.io.Serializable id) {
        try { return mapper.selectById(id); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return null; throw ex; }
    }

    private <T> T safeSelectOne(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, QueryWrapper<T> wrapper) {
        try { return mapper.selectOne(wrapper); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return null; throw ex; }
    }

    private <T> List<T> safeSelectList(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, QueryWrapper<T> wrapper) {
        try { return mapper.selectList(wrapper); }
        catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return List.of(); throw ex; }
    }

    private <T> Map<Long, T> selectByIds(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        try {
            return mapper.selectBatchIds(ids).stream()
                    .collect(java.util.stream.Collectors.toMap(AiRuntimeHelper::entityId, java.util.function.Function.identity(), (left, right) -> left, LinkedHashMap::new));
        } catch (RuntimeException ex) { if (AiRuntimeHelper.isMissingTable(ex)) return Map.of(); throw ex; }
    }
}