package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiChatBiDatasource;
import com.goat.cloud.module.ai.entity.AiSqlLog;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.mapper.AiSqlLogMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SQL 执行节点执行器
 * <p>
 * 执行 NL2SQL 生成的 SQL 查询，返回查询结果
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlExecutionExecutor implements NodeExecutor {

    private final AiChatBiDatasourceMapper datasourceMapper;
    private final AiSqlLogMapper sqlLogMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getNodeType() {
        return "SQL_EXECUTION";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            String generatedSql = (String) context.get("generatedSql");
            if (generatedSql == null || generatedSql.isBlank()) {
                return NodeResult.fail("No SQL to execute");
            }

            // 安全检查
            if (!isSafeSql(generatedSql)) {
                return NodeResult.fail("Only SELECT queries are allowed");
            }

            // 添加行数限制（防止返回过多数据）
            String limitedSql = applyRowLimit(generatedSql);

            long startTime = System.currentTimeMillis();

            // 执行 SQL
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(limitedSql);

            long executionTime = System.currentTimeMillis() - startTime;

            // 构建结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sql", generatedSql);
            result.put("rowCount", rows.size());
            result.put("rows", rows);
            result.put("executionTimeMs", executionTime);
            result.put("columns", rows.isEmpty() ? Collections.emptyList() : new ArrayList<>(rows.get(0).keySet()));

            // 更新 SQL 日志
            updateSqlLog(generatedSql, rows, executionTime, "SUCCESS", null);

            context.put("sqlResult", result);
            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("SQL execution error", e);
            updateSqlLog((String) context.get("generatedSql"), null, 0, "FAILED", e.getMessage());
            return NodeResult.fail("SQL execution failed: " + e.getMessage());
        }
    }

    private boolean isSafeSql(String sql) {
        if (sql == null || sql.isBlank()) return false;
        String upper = sql.trim().toUpperCase();
        // 1. 必须以 SELECT 或 WITH 开头
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) return false;
        // 2. 禁止分号（多语句注入）
        if (sql.contains(";")) return false;
        // 3. 禁止 DML/DDL 关键词
        String[] forbidden = {"INSERT ", "UPDATE ", "DELETE ", "DROP ", "ALTER ",
                "CREATE ", "TRUNCATE ", "EXEC ", "EXECUTE ", "GRANT ", "REVOKE ",
                "INTO OUTFILE ", "INTO DUMPFILE ", "LOAD DATA "};
        for (String kw : forbidden) {
            if (upper.contains(kw)) return false;
        }
        // 4. 禁止注释符号
        if (sql.contains("--") || sql.contains("/*")) return false;
        return true;
    }

    private String applyRowLimit(String sql) {
        String trimmed = sql.trim();
        if (trimmed.toUpperCase().endsWith("LIMIT 100")) {
            return trimmed;
        }
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + " LIMIT 100";
    }

    private void updateSqlLog(String sql, List<Map<String, Object>> rows, long executionTime, String status, String error) {
        if (sql == null) return;
        try {
            AiSqlLog logEntry = new AiSqlLog();
            logEntry.setGeneratedSql(sql);
            logEntry.setExecutionResult(rows != null ? objectMapper.writeValueAsString(rows) : null);
            logEntry.setExecutionTimeMs(executionTime);
            logEntry.setStatus(status);
            logEntry.setErrorMessage(error);
            logEntry.setCreateTime(LocalDateTime.now());
            logEntry.setDeleted(0);
            sqlLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("Failed to update SQL log", e);
        }
    }
}