-- AI Model Config 扩展字段
ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS api_key_encrypted VARCHAR(512);
ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS api_key_version VARCHAR(32);
ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS retry_config JSONB;
ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS rate_limit_config JSONB;
ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS capability_tags VARCHAR(256);
ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS price_config JSONB;

CREATE INDEX IF NOT EXISTS idx_model_config_provider ON ai_model_config(provider);
CREATE INDEX IF NOT EXISTS idx_model_config_type ON ai_model_config(model_type);
CREATE INDEX IF NOT EXISTS idx_model_config_status ON ai_model_config(status);

-- AI Knowledge Base 扩展字段
ALTER TABLE ai_knowledge_base ADD COLUMN IF NOT EXISTS retrieval_config JSONB;
ALTER TABLE ai_knowledge_base ADD COLUMN IF NOT EXISTS indexing_config JSONB;
ALTER TABLE ai_knowledge_base ADD COLUMN IF NOT EXISTS access_control JSONB;
ALTER TABLE ai_knowledge_base ADD COLUMN IF NOT EXISTS metadata_fields VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_knowledge_base_code ON ai_knowledge_base(knowledge_base_code);

-- AI Document 扩展字段
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS storage_path VARCHAR(512);
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS storage_bucket VARCHAR(128);
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS file_hash VARCHAR(64);
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS parse_error TEXT;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS page_count INTEGER;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS word_count INTEGER;

CREATE INDEX IF NOT EXISTS idx_document_knowledge_base ON ai_document(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_document_type ON ai_document(document_type);
CREATE INDEX IF NOT EXISTS idx_document_parse_status ON ai_document(parse_status);

-- AI Document Chunk 扩展字段
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS vector_id VARCHAR(128);
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS start_position INTEGER;
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS end_position INTEGER;

CREATE INDEX IF NOT EXISTS idx_chunk_knowledge_base ON ai_document_chunk(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_chunk_document ON ai_document_chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_status ON ai_document_chunk(embedding_status);

-- AI Prompt Template 扩展字段
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS parent_version_id BIGINT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS version_status VARCHAR(32) DEFAULT 'DRAFT';
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS usage_count INTEGER DEFAULT 0;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS effect_start_time TIMESTAMP;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS effect_end_time TIMESTAMP;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS variables_schema JSONB;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS output_format VARCHAR(64);
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS test_cases TEXT;

CREATE INDEX IF NOT EXISTS idx_prompt_code ON ai_prompt_template(prompt_code);
CREATE INDEX IF NOT EXISTS idx_prompt_version ON ai_prompt_template(version);
CREATE INDEX IF NOT EXISTS idx_prompt_status ON ai_prompt_template(status);

-- AI MCP Tool 扩展字段
ALTER TABLE ai_mcp_tool ADD COLUMN IF NOT EXISTS auth_config JSONB;
ALTER TABLE ai_mcp_tool ADD COLUMN IF NOT EXISTS timeout_config JSONB;
ALTER TABLE ai_mcp_tool ADD COLUMN IF NOT EXISTS retry_config JSONB;
ALTER TABLE ai_mcp_tool ADD COLUMN IF NOT EXISTS health_status VARCHAR(32) DEFAULT 'UNKNOWN';
ALTER TABLE ai_mcp_tool ADD COLUMN IF NOT EXISTS last_health_check TIMESTAMP;
ALTER TABLE ai_mcp_tool ADD COLUMN IF NOT EXISTS tool_category VARCHAR(64);
ALTER TABLE ai_mcp_tool ADD COLUMN IF NOT EXISTS capability_tags VARCHAR(256);

CREATE INDEX IF NOT EXISTS idx_mcp_tool_server ON ai_mcp_tool(server_name);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_transport ON ai_mcp_tool(transport_type);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_category ON ai_mcp_tool(tool_category);

-- Secret Management Table
CREATE TABLE IF NOT EXISTS ai_secret (
    secret_id BIGSERIAL PRIMARY KEY,
    secret_key VARCHAR(128) NOT NULL UNIQUE,
    secret_value_encrypted TEXT NOT NULL,
    secret_type VARCHAR(32) NOT NULL,
    model_id BIGINT,
    mcp_tool_id BIGINT,
    version INTEGER DEFAULT 1,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_secret_key ON ai_secret(secret_key);
CREATE INDEX IF NOT EXISTS idx_secret_model ON ai_secret(model_id);
CREATE INDEX IF NOT EXISTS idx_secret_mcp ON ai_secret(mcp_tool_id);

-- Prompt Version History Table
CREATE TABLE IF NOT EXISTS ai_prompt_version (
    version_id BIGSERIAL PRIMARY KEY,
    prompt_id BIGINT NOT NULL,
    version VARCHAR(32) NOT NULL,
    system_prompt TEXT,
    user_prompt TEXT,
    variables_schema JSONB,
    version_status VARCHAR(32) DEFAULT 'DRAFT',
    change_summary TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(prompt_id, version)
);

CREATE INDEX IF NOT EXISTS idx_prompt_version_prompt ON ai_prompt_version(prompt_id);

-- MCP Connection Log Table
CREATE TABLE IF NOT EXISTS ai_mcp_connection_log (
    log_id BIGSERIAL PRIMARY KEY,
    mcp_tool_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    error_message TEXT,
    duration_ms BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mcp_log_tool ON ai_mcp_connection_log(mcp_tool_id);
CREATE INDEX IF NOT EXISTS idx_mcp_log_time ON ai_mcp_connection_log(create_time);
