-- V17: 创建AI请求日志表
-- 用于记录每次AI请求的详细信息，支持监控、审计和成本分析

CREATE TABLE IF NOT EXISTS ai_request_log (
    log_id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64),
    user_id BIGINT,
    model_id BIGINT,
    model_name VARCHAR(128),
    provider VARCHAR(64),
    biz_type VARCHAR(32),
    request_id VARCHAR(64) UNIQUE NOT NULL,
    prompt_tokens INTEGER DEFAULT 0,
    completion_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    latency_ms BIGINT,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    client_ip VARCHAR(64),
    request_time TIMESTAMP NOT NULL,
    response_time TIMESTAMP,
    stream BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_ai_request_log_user_id ON ai_request_log(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_request_log_model_id ON ai_request_log(model_id);
CREATE INDEX IF NOT EXISTS idx_ai_request_log_conversation_id ON ai_request_log(conversation_id);
CREATE INDEX IF NOT EXISTS idx_ai_request_log_request_time ON ai_request_log(request_time);
CREATE INDEX IF NOT EXISTS idx_ai_request_log_status ON ai_request_log(status);
CREATE INDEX IF NOT EXISTS idx_ai_request_log_request_id ON ai_request_log(request_id);

-- 添加表注释
COMMENT ON TABLE ai_request_log IS 'AI请求日志表 - 记录每次AI调用的详细信息';
COMMENT ON COLUMN ai_request_log.log_id IS '日志ID';
COMMENT ON COLUMN ai_request_log.conversation_id IS '会话ID';
COMMENT ON COLUMN ai_request_log.user_id IS '用户ID';
COMMENT ON COLUMN ai_request_log.model_id IS '模型ID';
COMMENT ON COLUMN ai_request_log.model_name IS '模型名称';
COMMENT ON COLUMN ai_request_log.provider IS '模型提供商';
COMMENT ON COLUMN ai_request_log.biz_type IS '业务类型(chat/completion/embedding等)';
COMMENT ON COLUMN ai_request_log.request_id IS '请求唯一ID';
COMMENT ON COLUMN ai_request_log.prompt_tokens IS '输入Token数';
COMMENT ON COLUMN ai_request_log.completion_tokens IS '输出Token数';
COMMENT ON COLUMN ai_request_log.total_tokens IS '总Token数';
COMMENT ON COLUMN ai_request_log.latency_ms IS '请求耗时(毫秒)';
COMMENT ON COLUMN ai_request_log.status IS '请求状态(SUCCESS/FAILED/TIMEOUT/IN_PROGRESS)';
COMMENT ON COLUMN ai_request_log.error_message IS '错误信息';
COMMENT ON COLUMN ai_request_log.client_ip IS '客户端IP';
COMMENT ON COLUMN ai_request_log.request_time IS '请求时间';
COMMENT ON COLUMN ai_request_log.response_time IS '响应时间';
COMMENT ON COLUMN ai_request_log.stream IS '是否流式请求';
COMMENT ON COLUMN ai_request_log.metadata IS '额外元数据(JSON格式)';
