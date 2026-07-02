-- V16: 为 ai_conversation_record 表添加模型统计相关字段
-- 用于记录每次对话使用的模型ID和Token消耗详情

-- 添加模型ID字段
ALTER TABLE ai_conversation_record 
ADD COLUMN IF NOT EXISTS model_id BIGINT;

-- 添加提示词Token数字段
ALTER TABLE ai_conversation_record 
ADD COLUMN IF NOT EXISTS prompt_tokens INTEGER;

-- 添加完成Token数字段
ALTER TABLE ai_conversation_record 
ADD COLUMN IF NOT EXISTS completion_tokens INTEGER;

-- 为model_id添加索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_ai_conversation_record_model_id 
ON ai_conversation_record(model_id);

-- 添加注释
COMMENT ON COLUMN ai_conversation_record.model_id IS '使用的模型ID';
COMMENT ON COLUMN ai_conversation_record.prompt_tokens IS '提示词消耗的Token数';
COMMENT ON COLUMN ai_conversation_record.completion_tokens IS '模型生成内容消耗的Token数';
