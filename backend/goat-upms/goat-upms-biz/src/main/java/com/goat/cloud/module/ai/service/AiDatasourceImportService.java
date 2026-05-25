package com.goat.cloud.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.module.ai.entity.AiChatBiDatasource;
import com.goat.cloud.module.ai.entity.AiChatBiTable;
import com.goat.cloud.module.ai.mapper.AiChatBiDatasourceMapper;
import com.goat.cloud.module.ai.mapper.AiChatBiTableMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Datasource JDBC import service — reads table/column metadata from external databases
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDatasourceImportService {

    private final AiChatBiDatasourceMapper datasourceMapper;
    private final AiChatBiTableMapper tableMapper;
    private final ObjectMapper objectMapper;

    /**
     * Test JDBC connection to a datasource
     */
    public Map<String, Object> testConnection(Long datasourceId) {
        AiChatBiDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException(4044, "Datasource not found");

        try (Connection conn = createConnection(ds)) {
            DatabaseMetaData meta = conn.getMetaData();
            return Map.of(
                "connected", true,
                "databaseProductName", meta.getDatabaseProductName(),
                "databaseProductVersion", meta.getDatabaseProductVersion(),
                "driverName", meta.getDriverName(),
                "driverVersion", meta.getDriverVersion(),
                "url", meta.getURL()
            );
        } catch (SQLException e) {
            return Map.of("connected", false, "error", e.getMessage());
        }
    }

    /**
     * List available schemas from a datasource
     */
    public List<String> listSchemas(Long datasourceId) {
        AiChatBiDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException(4044, "Datasource not found");

        try (Connection conn = createConnection(ds)) {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> schemas = new ArrayList<>();
            ResultSet rs = meta.getSchemas();
            while (rs.next()) {
                schemas.add(rs.getString("TABLE_SCHEM"));
            }
            rs.close();
            return schemas;
        } catch (SQLException e) {
            throw new BusinessException(5000, "Failed to list schemas: " + e.getMessage());
        }
    }

    /**
     * Import tables from a datasource's JDBC connection into ai_chatbi_table
     */
    public List<AiChatBiTable> importTables(Long datasourceId, String schemaName) {
        AiChatBiDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException(4044, "Datasource not found");

        String schema = schemaName != null ? schemaName : "public";
        List<AiChatBiTable> imported = new ArrayList<>();

        try (Connection conn = createConnection(ds)) {
            DatabaseMetaData meta = conn.getMetaData();

            // Get all tables in the schema
            ResultSet tablesRs = meta.getTables(null, schema, "%", new String[]{"TABLE", "VIEW"});
            while (tablesRs.next()) {
                String tableName = tablesRs.getString("TABLE_NAME");
                String tableComment = tablesRs.getString("REMARKS");

                // Get columns for this table
                List<Map<String, Object>> columns = new ArrayList<>();
                ResultSet colsRs = meta.getColumns(null, schema, tableName, "%");
                while (colsRs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("columnName", colsRs.getString("COLUMN_NAME"));
                    col.put("dataType", colsRs.getString("TYPE_NAME"));
                    col.put("columnSize", colsRs.getInt("COLUMN_SIZE"));
                    col.put("nullable", colsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    col.put("columnComment", colsRs.getString("REMARKS"));
                    col.put("ordinalPosition", colsRs.getInt("ORDINAL_POSITION"));
                    columns.add(col);
                }
                colsRs.close();

                String columnsJson = objectMapper.writeValueAsString(columns);

                // Check if table already exists in our records
                AiChatBiTable existing = tableMapper.selectOne(
                    new LambdaQueryWrapper<AiChatBiTable>()
                        .eq(AiChatBiTable::getDatasourceId, datasourceId)
                        .eq(AiChatBiTable::getSchemaName, schema)
                        .eq(AiChatBiTable::getTableName, tableName)
                        .last("limit 1")
                );

                if (existing != null) {
                    // Update existing record
                    existing.setTableComment(tableComment != null && !tableComment.isBlank() ? tableComment : tableName);
                    existing.setColumnsJson(columnsJson);
                    tableMapper.updateById(existing);
                    imported.add(existing);
                } else {
                    // Create new record
                    AiChatBiTable newTable = new AiChatBiTable();
                    newTable.setDatasourceId(datasourceId);
                    newTable.setSchemaName(schema);
                    newTable.setTableName(tableName);
                    newTable.setTableComment(tableComment != null && !tableComment.isBlank() ? tableComment : tableName);
                    newTable.setColumnsJson(columnsJson);
                    newTable.setStatus(com.goat.cloud.common.enums.CommonStatus.ENABLED);
                    tableMapper.insert(newTable);
                    imported.add(newTable);
                }
            }
            tablesRs.close();
        } catch (SQLException e) {
            throw new BusinessException(5000, "JDBC import failed: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(5000, "Import processing failed: " + e.getMessage());
        }

        return imported;
    }

    private Connection createConnection(AiChatBiDatasource ds) throws SQLException {
        String driverClass = ds.getDriverClassName() != null ? ds.getDriverClassName() : "org.postgresql.Driver";
        String password = resolvePassword(ds);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setUrl(ds.getJdbcUrl());
        dataSource.setUsername(ds.getUsername());
        dataSource.setPassword(password);
        return dataSource.getConnection();
    }

    private String resolvePassword(AiChatBiDatasource ds) {
        // 1. passwordEncrypted field (future: AES decryption via AiApiKeyManager)
        // 2. credentialRef with ENV: prefix — environment variable
        // 3. credentialRef as plain password (dev mode)
        if (ds.getPasswordEncrypted() != null && !ds.getPasswordEncrypted().isBlank()) {
            return ds.getPasswordEncrypted();
        }
        if (ds.getCredentialRef() != null && !ds.getCredentialRef().isBlank()) {
            if (ds.getCredentialRef().startsWith("ENV:")) {
                String envVar = ds.getCredentialRef().substring(4);
                String envValue = System.getenv(envVar);
                return envValue != null ? envValue : "";
            }
            return ds.getCredentialRef();
        }
        return "";
    }
}
