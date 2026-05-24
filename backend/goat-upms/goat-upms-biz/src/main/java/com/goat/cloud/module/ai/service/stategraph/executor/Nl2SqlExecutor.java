package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiSqlLog;
import com.goat.cloud.module.ai.mapper.AiSqlLogMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import com.goat.cloud.module.ai.runtime.AiRuntimeService;
import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * NL2SQL 节点执行器
 * <p>
 * 将自然语言问题转换为 SQL 查询
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Nl2SqlExecutor implements NodeExecutor {

    private final AiRuntimeService runtimeService;
    private final AiSqlLogMapper sqlLogMapper;
    private final ObjectMapper objectMapper;

    private static final String NL2SQL_SYSTEM_PROMPT = """
            你是一个专业的SQL生成助手。根据用户的自然语言问题和数据库表结构，生成正确的SQL查询。

            规则：
            1. 只生成SELECT查询，不允许生成INSERT/UPDATE/DELETE
            2. 使用标准SQL语法，兼容PostgreSQL
            3. 对日期字段使用合适的格式化
            4. 对聚合查询添加合理的GROUP BY
            5. 添加必要的WHERE条件过滤
            6. 限制返回行数（最多100行）
            7. 只返回SQL语句，不要任何解释

            输出格式：
            ```sql
            -- 你的SQL语句
            ```
            """;

    @Override
    public String getNodeType() {
        return "NL2SQL";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            String question = (String) context.getOrDefault("question", "");
            Object schemaRecallResult = context.get("schemaRecallResult");
            String intent = (String) context.getOrDefault("intent", "DATA_QUERY");

            if (question == null || question.isBlank()) {
                return NodeResult.fail("Question is empty");
            }

            // 构建 prompt
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("用户问题：").append(question).append("\n\n");

            if (schemaRecallResult != null) {
                userPrompt.append("可用的数据库表结构：\n");
                userPrompt.append(objectMapper.writeValueAsString(schemaRecallResult)).append("\n\n");
            }

            userPrompt.append("意图类型：").append(intent).append("\n");
            userPrompt.append("请生成对应的SQL查询语句。");

            // 调用 LLM 生成 SQL
            AiChatRequest chatRequest = new AiChatRequest();
            chatRequest.setSystemPrompt(NL2SQL_SYSTEM_PROMPT);
            chatRequest.setMessage(userPrompt.toString());
            chatRequest.setOptions(Map.of("bizType", "NL2SQL"));
            AiChatResponse chatResponse = runtimeService.chat(chatRequest);
            String generatedSql = chatResponse.getMessage() != null ? chatResponse.getMessage().getContent() : "";

            // 清理 LLM 输出中的 markdown 标记
            generatedSql = cleanSqlOutput(generatedSql);

            // 安全检查：只允许 SELECT 语句
            if (!isSafeSql(generatedSql)) {
                return NodeResult.fail("Generated SQL is not a safe SELECT query");
            }

            // 记录 SQL 日志
            Long sessionId = context.get("sessionId") != null ? toLong(context.get("sessionId")) : null;
            saveSqlLog(sessionId, question, generatedSql, intent, schemaRecallResult);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("generatedSql", generatedSql);
            result.put("intent", intent);

            context.put("generatedSql", generatedSql);
            context.put("nl2sqlResult", result);

            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("NL2SQL error", e);
            return NodeResult.fail("NL2SQL failed: " + e.getMessage());
        }
    }

    private String cleanSqlOutput(String output) {
        if (output == null) return "";
        // 移除 markdown 代码块标记
        String sql = output.trim();
        if (sql.startsWith("```sql")) {
            sql = sql.substring(6);
        } else if (sql.startsWith("```")) {
            sql = sql.substring(3);
        }
        if (sql.endsWith("```")) {
            sql = sql.substring(0, sql.length() - 3);
        }
        return sql.trim();
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

    private void saveSqlLog(Long sessionId, String question, String generatedSql, String intent, Object schemaContext) {
        try {
            AiSqlLog sqlLog = new AiSqlLog();
            sqlLog.setSessionId(sessionId);
            sqlLog.setQuestion(question);
            sqlLog.setGeneratedSql(generatedSql);
            sqlLog.setIntentResult(intent);
            sqlLog.setStatus("GENERATED");
            sqlLog.setCreateTime(LocalDateTime.now());
            sqlLog.setDeleted(0);
            sqlLogMapper.insert(sqlLog);
        } catch (Exception e) {
            log.warn("Failed to save SQL log", e);
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