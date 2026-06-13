-- AI Conversation Persistence: 会话与消息记录
-- 参考 snail-ai 的 conversation + conversation_record 模式

-- 会话表：记录每个 Agent 的对话会话
CREATE TABLE IF NOT EXISTS ai_conversation (
    conversation_id VARCHAR(64) PRIMARY KEY,
    agent_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(128),
    status          VARCHAR(16) DEFAULT 'ACTIVE',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_conversation_agent_user ON ai_conversation(agent_id, user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_user ON ai_conversation(user_id);

-- 会话记录表：存储每条消息
CREATE TABLE IF NOT EXISTS ai_conversation_record (
    record_id       BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    agent_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    role            VARCHAR(16) NOT NULL,
    content         TEXT NOT NULL,
    thinking        TEXT,
    token_count     INTEGER DEFAULT 0,
    status          VARCHAR(16) DEFAULT 'SUCCESS',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_conv_record_conv_id ON ai_conversation_record(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conv_record_agent_user ON ai_conversation_record(agent_id, user_id);

-- Seed: admin user conversations (id=1 for admin)
-- 无预置数据，运行时动态创建
