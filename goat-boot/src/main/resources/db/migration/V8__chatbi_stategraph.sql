-- ============================================
-- V8: ChatBI StateGraph Workflow Engine
-- ============================================

-- 1. StateGraph Definition Table
CREATE TABLE IF NOT EXISTS ai_stategraph (
    graph_id          BIGSERIAL PRIMARY KEY,
    graph_code        VARCHAR(100) NOT NULL UNIQUE,
    graph_name        VARCHAR(200) NOT NULL,
    description       VARCHAR(500),
    version           VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    definition_json   JSONB NOT NULL,
    config_json       JSONB,
    status            VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_stategraph_code ON ai_stategraph(graph_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_stategraph_status ON ai_stategraph(status);

-- 2. StateGraph Node Types Table
CREATE TABLE IF NOT EXISTS ai_state_node (
    node_id           BIGSERIAL PRIMARY KEY,
    graph_id          BIGINT NOT NULL,
    node_code         VARCHAR(100) NOT NULL,
    node_name         VARCHAR(200) NOT NULL,
    node_type         VARCHAR(50) NOT NULL,
    config_json       JSONB,
    input_schema      JSONB,
    output_schema     JSONB,
    retry_config      JSONB,
    timeout_ms        INTEGER DEFAULT 30000,
    sort_order        INTEGER NOT NULL DEFAULT 0,
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0,
    UNIQUE(graph_id, node_code)
);

CREATE INDEX IF NOT EXISTS idx_node_graph ON ai_state_node(graph_id);
CREATE INDEX IF NOT EXISTS idx_node_type ON ai_state_node(node_type);

-- 3. StateGraph Execution Session Table
CREATE TABLE IF NOT EXISTS ai_state_session (
    session_id        BIGSERIAL PRIMARY KEY,
    graph_id          BIGINT NOT NULL,
    run_id            VARCHAR(100) NOT NULL UNIQUE,
    user_id           BIGINT,
    conversation_id   VARCHAR(100),
    status            VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    current_node_id   BIGINT,
    interrupt_reason  VARCHAR(100),
    interrupt_data    JSONB,
    context_json      JSONB,
    result_json       JSONB,
    error_message     TEXT,
    started_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    interrupted_at    TIMESTAMP,
    completed_at      TIMESTAMP,
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_session_graph ON ai_state_session(graph_id);
CREATE INDEX IF NOT EXISTS idx_session_run ON ai_state_session(run_id);
CREATE INDEX IF NOT EXISTS idx_session_status ON ai_state_session(status);
CREATE INDEX IF NOT EXISTS idx_session_user ON ai_state_session(user_id);

-- 4. StateGraph Node Execution Trace Table
CREATE TABLE IF NOT EXISTS ai_state_trace (
    trace_id          BIGSERIAL PRIMARY KEY,
    session_id        BIGINT NOT NULL,
    node_id          BIGINT,
    node_code         VARCHAR(100) NOT NULL,
    node_type         VARCHAR(50) NOT NULL,
    input_json        JSONB,
    output_json       JSONB,
    status            VARCHAR(32) NOT NULL,
    error_message     TEXT,
    started_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at      TIMESTAMP,
    duration_ms       BIGINT,
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_trace_session ON ai_state_trace(session_id);
CREATE INDEX IF NOT EXISTS idx_trace_node ON ai_state_trace(node_code);
CREATE INDEX IF NOT EXISTS idx_trace_status ON ai_state_trace(status);

-- 5. Intent Recognition Config Table
CREATE TABLE IF NOT EXISTS ai_intent_config (
    config_id         BIGSERIAL PRIMARY KEY,
    intent_code       VARCHAR(100) NOT NULL UNIQUE,
    intent_name       VARCHAR(200) NOT NULL,
    description       VARCHAR(500),
    prompt_template   TEXT NOT NULL,
    examples_json     JSONB,
    model_config_json JSONB,
    threshold_score   DECIMAL(5,4) DEFAULT 0.7500,
    fallback_intent   VARCHAR(100),
    status            VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_intent_code ON ai_intent_config(intent_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_intent_status ON ai_intent_config(status);

-- 6. Schema Cache Table (for NL2SQL Schema Recall)
CREATE TABLE IF NOT EXISTS ai_schema_cache (
    cache_id          BIGSERIAL PRIMARY KEY,
    datasource_id     BIGINT NOT NULL,
    cache_key         VARCHAR(255) NOT NULL,
    schema_snapshot   JSONB NOT NULL,
    sample_queries    JSONB,
    hit_count         INTEGER NOT NULL DEFAULT 0,
    last_hit_at       TIMESTAMP,
    expires_at        TIMESTAMP,
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0,
    UNIQUE(datasource_id, cache_key)
);

CREATE INDEX IF NOT EXISTS idx_schema_cache_datasource ON ai_schema_cache(datasource_id);
CREATE INDEX IF NOT EXISTS idx_schema_cache_hit ON ai_schema_cache(hit_count DESC);

-- 7. SQL Generation Log Table
CREATE TABLE IF NOT EXISTS ai_sql_log (
    log_id            BIGSERIAL PRIMARY KEY,
    session_id        BIGINT,
    question          TEXT NOT NULL,
    generated_sql     TEXT,
    intent_result     JSONB,
    schema_context    JSONB,
    llm_config_json   JSONB,
    execution_result  JSONB,
    execution_time_ms BIGINT,
    status            VARCHAR(32) NOT NULL,
    error_message     TEXT,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sql_log_session ON ai_sql_log(session_id);
CREATE INDEX IF NOT EXISTS idx_sql_log_status ON ai_sql_log(status);
CREATE INDEX IF NOT EXISTS idx_sql_log_create ON ai_sql_log(create_time DESC);

-- 8. Human Feedback Table (for HITL)
CREATE TABLE IF NOT EXISTS ai_feedback (
    feedback_id       BIGSERIAL PRIMARY KEY,
    session_id        BIGINT NOT NULL,
    node_id          BIGINT,
    feedback_type     VARCHAR(50) NOT NULL,
    rating            INTEGER,
    correction_json   JSONB,
    comment           TEXT,
    status            VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_feedback_session ON ai_feedback(session_id);
CREATE INDEX IF NOT EXISTS idx_feedback_type ON ai_feedback(feedback_type);
CREATE INDEX IF NOT EXISTS idx_feedback_status ON ai_feedback(status);

-- 9. Python Execution Config Table
CREATE TABLE IF NOT EXISTS ai_python_config (
    config_id         BIGSERIAL PRIMARY KEY,
    config_code       VARCHAR(100) NOT NULL UNIQUE,
    config_name       VARCHAR(200) NOT NULL,
    execution_mode    VARCHAR(32) NOT NULL DEFAULT 'DOCKER',
    docker_image      VARCHAR(255),
    working_dir       VARCHAR(500),
    env_vars_json     JSONB,
    dependencies_json JSONB,
    timeout_seconds   INTEGER DEFAULT 60,
    memory_limit_mb   INTEGER DEFAULT 512,
    status            VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_python_config_code ON ai_python_config(config_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_python_config_mode ON ai_python_config(execution_mode);

-- 10. Report Template Table (for ECharts Report Generation)
CREATE TABLE IF NOT EXISTS ai_report_template (
    template_id       BIGSERIAL PRIMARY KEY,
    template_code     VARCHAR(100) NOT NULL UNIQUE,
    template_name     VARCHAR(200) NOT NULL,
    description       VARCHAR(500),
    chart_type        VARCHAR(50) NOT NULL,
    template_json     JSONB NOT NULL,
    default_options   JSONB,
    data_mapping_json JSONB,
    status            VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_report_template_code ON ai_report_template(template_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_report_template_type ON ai_report_template(chart_type);

-- 11. MCP Server Registry Table
CREATE TABLE IF NOT EXISTS ai_mcp_server (
    server_id         BIGSERIAL PRIMARY KEY,
    server_code       VARCHAR(100) NOT NULL UNIQUE,
    server_name       VARCHAR(200) NOT NULL,
    transport_type    VARCHAR(32) NOT NULL DEFAULT 'stdio',
    endpoint          VARCHAR(500),
    auth_config_json  JSONB,
    capabilities_json JSONB,
    health_status     VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    last_health_check TIMESTAMP,
    status            VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mcp_server_code ON ai_mcp_server(server_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_server_health ON ai_mcp_server(health_status);

-- 12. Logical Relation Table (for multi-table JOIN support)
CREATE TABLE IF NOT EXISTS ai_chatbi_logical_relation (
    relation_id       BIGSERIAL PRIMARY KEY,
    dataset_id        BIGINT NOT NULL,
    left_table        VARCHAR(100) NOT NULL,
    left_column       VARCHAR(100) NOT NULL,
    right_table       VARCHAR(100) NOT NULL,
    right_column      VARCHAR(100) NOT NULL,
    relation_type     VARCHAR(50) NOT NULL,
    description       VARCHAR(500),
    create_by         BIGINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT NOT NULL DEFAULT 0,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0,
    UNIQUE(dataset_id, left_table, left_column, right_table, right_column)
);

CREATE INDEX IF NOT EXISTS idx_logical_relation_dataset ON ai_chatbi_logical_relation(dataset_id);
CREATE INDEX IF NOT EXISTS idx_logical_relation_tables ON ai_chatbi_logical_relation(left_table, right_table);
