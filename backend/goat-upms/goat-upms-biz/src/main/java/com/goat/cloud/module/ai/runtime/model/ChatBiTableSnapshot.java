package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class ChatBiTableSnapshot {

    private Long tableId;
    private Long datasourceId;
    private String schemaName;
    private String tableName;
    private String tableComment;
    private List<Map<String, Object>> columns;
}
