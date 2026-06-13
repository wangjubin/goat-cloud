-- =============================================================================
-- Goat Cloud AI 模型配置种子数据
-- =============================================================================
-- 用法:
--   psql -h localhost -U postgres -d goat_cloud -f seed-model-config-example.sql
-- 或在 Navicat/DBeaver 等 GUI 中直接打开执行。
--
-- 安全说明:
--   1. 本脚本**绝不**写入明文 API Key,全部使用 ENV:/PROP: 引用
--   2. 执行前请先把真实 Key 注入到运行环境或 application*.yml
--   3. 全部使用 ON CONFLICT DO NOTHING,执行多次幂等,不会覆盖已有配置
--
-- 字段约定:
--   - modelCode     唯一,运行时通过它查找模型(已存在的同名记录不会被覆盖)
--   - modelType     CHAT(对话) / EMBEDDING(向量化) / RERANK(重排)
--   - apiKeyRef     ENV:NAME / PROP:path / ${name} / VALUE:xxx(慎用)
--   - contextWindow 模型上下文长度(对 ChatBI/RAG 长文档很关键)
--   - defaultModel  同类型下只允许一个为 true,平台会用作 fallback
--   - capabilityTags  逗号分隔的能力标签(chat/streaming/json/function-call/vision)
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. CHAT 模型(对话)
-- -----------------------------------------------------------------------------

-- 1.1 OpenAI
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('gpt-4o-mini', 'GPT-4o mini', 'OpenAI', 'CHAT',
   'https://api.openai.com/v1', 'ENV:OPENAI_API_KEY', 128000, true,
   'chat,streaming,json,function-call', 'ENABLED', 10,
   'OpenAI 主力对话模型,性价比高,支持函数调用与 JSON 模式'),
  ('gpt-4o', 'GPT-4o', 'OpenAI', 'CHAT',
   'https://api.openai.com/v1', 'ENV:OPENAI_API_KEY', 128000, false,
   'chat,streaming,json,function-call,vision', 'ENABLED', 11,
   'OpenAI 多模态旗舰,128K 上下文,支持图像理解'),
  ('o1-preview', 'o1-preview', 'OpenAI', 'CHAT',
   'https://api.openai.com/v1', 'ENV:OPENAI_API_KEY', 128000, false,
   'chat,reasoning,streaming', 'ENABLED', 12,
   'OpenAI 推理模型,适合复杂逻辑与数学任务')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 1.2 DeepSeek
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('deepseek-chat', 'DeepSeek-V3', 'DeepSeek', 'CHAT',
   'https://api.deepseek.com/v1', 'ENV:DEEPSEEK_API_KEY', 64000, false,
   'chat,streaming,json,function-call', 'ENABLED', 20,
   'DeepSeek-V3 主力对话模型,中文能力强,价格低'),
  ('deepseek-reasoner', 'DeepSeek-R1', 'DeepSeek', 'CHAT',
   'https://api.deepseek.com/v1', 'ENV:DEEPSEEK_API_KEY', 64000, false,
   'chat,reasoning,streaming', 'ENABLED', 21,
   'DeepSeek-R1 推理模型,适合多步推理与代码生成')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 1.3 Anthropic Claude
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('claude-3-5-sonnet', 'Claude 3.5 Sonnet', 'Anthropic', 'CHAT',
   'https://api.anthropic.com/v1', 'ENV:ANTHROPIC_API_KEY', 200000, false,
   'chat,streaming,json,function-call,vision', 'ENABLED', 30,
   'Claude 3.5 Sonnet,200K 上下文,代码与长文档首选'),
  ('claude-3-haiku', 'Claude 3 Haiku', 'Anthropic', 'CHAT',
   'https://api.anthropic.com/v1', 'ENV:ANTHROPIC_API_KEY', 200000, false,
   'chat,streaming,json', 'ENABLED', 31,
   'Claude 3 Haiku,快速低延迟,适合简单问答')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 1.4 通义千问 (DashScope OpenAI 兼容模式)
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('qwen-plus', '通义千问 Plus', '通义千问', 'CHAT',
   'https://dashscope.aliyuncs.com/compatible-mode/v1', 'ENV:DASHSCOPE_API_KEY', 128000, false,
   'chat,streaming,json,function-call', 'ENABLED', 40,
   '通义千问 Plus,128K 上下文,中文场景优秀'),
  ('qwen-turbo', '通义千问 Turbo', '通义千问', 'CHAT',
   'https://dashscope.aliyuncs.com/compatible-mode/v1', 'ENV:DASHSCOPE_API_KEY', 1000000, false,
   'chat,streaming', 'ENABLED', 41,
   '通义千问 Turbo,百万上下文,适合超长文档分析')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 1.5 智谱 GLM
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('glm-4-plus', 'GLM-4 Plus', '智谱', 'CHAT',
   'https://open.bigmodel.cn/api/paas/v4', 'ENV:ZHIPUAI_API_KEY', 128000, false,
   'chat,streaming,json,function-call', 'ENABLED', 50,
   '智谱 GLM-4 Plus,128K 上下文,函数调用与工具调用'),
  ('glm-4-flash', 'GLM-4 Flash', '智谱', 'CHAT',
   'https://open.bigmodel.cn/api/paas/v4', 'ENV:ZHIPUAI_API_KEY', 128000, false,
   'chat,streaming', 'ENABLED', 51,
   '智谱 GLM-4 Flash,免费高速,适合简单问答')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 1.6 Ollama (本地部署,无需 Key)
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('llama3.1-8b', 'Llama 3.1 8B (本地)', 'Ollama', 'CHAT',
   'http://localhost:11434/v1', 'VALUE:ollama', 128000, false,
   'chat,streaming', 'ENABLED', 80,
   '本地 Ollama 部署的 Llama 3.1,离线可用,适合隐私场景'),
  ('qwen2-7b', 'Qwen2 7B (本地)', 'Ollama', 'CHAT',
   'http://localhost:11434/v1', 'VALUE:ollama', 32000, false,
   'chat,streaming', 'ENABLED', 81,
   '本地 Ollama 部署的 Qwen2,中文效果优于 Llama')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;


-- -----------------------------------------------------------------------------
-- 2. EMBEDDING 模型(向量化)
-- -----------------------------------------------------------------------------

-- 2.1 OpenAI Embedding
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('text-embedding-3-small', 'text-embedding-3-small', 'OpenAI', 'EMBEDDING',
   'https://api.openai.com/v1', 'ENV:OPENAI_API_KEY', 8192, true,
   'embedding,batch', 'ENABLED', 100,
   'OpenAI 第三代 Embedding,1536 维,性价比高,推荐默认'),
  ('text-embedding-3-large', 'text-embedding-3-large', 'OpenAI', 'EMBEDDING',
   'https://api.openai.com/v1', 'ENV:OPENAI_API_KEY', 8192, false,
   'embedding,batch', 'ENABLED', 101,
   'OpenAI 大尺寸 Embedding,3072 维,精度更高')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 2.2 通义 Embedding
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('text-embedding-v3', '通义 text-embedding-v3', '通义千问', 'EMBEDDING',
   'https://dashscope.aliyuncs.com/compatible-mode/v1', 'ENV:DASHSCOPE_API_KEY', 8192, false,
   'embedding,batch,multilingual', 'ENABLED', 110,
   '通义第三代 Embedding,1024 维,中英双语优化'),
  ('text-embedding-v2', '通义 text-embedding-v2', '通义千问', 'EMBEDDING',
   'https://dashscope.aliyuncs.com/compatible-mode/v1', 'ENV:DASHSCOPE_API_KEY', 2048, false,
   'embedding,batch', 'ENABLED', 111,
   '通义第二代 Embedding,1536 维,稳定版本')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 2.3 BGE (智谱/开源)
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('bge-large-zh-v1.5', 'BGE Large 中文 v1.5', 'BGE', 'EMBEDDING',
   'http://localhost:11434/v1', 'VALUE:ollama', 512, false,
   'embedding,batch,chinese', 'ENABLED', 120,
   '智源 BGE 中文大模型 Embedding,1024 维,需本地部署'),
  ('bge-m3', 'BGE-M3 多语言', 'BGE', 'EMBEDDING',
   'http://localhost:11434/v1', 'VALUE:ollama', 8192, false,
   'embedding,batch,multilingual,long-text', 'ENABLED', 121,
   'BGE-M3 多语言 Embedding,1024 维,支持 8K 长文本')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;


-- -----------------------------------------------------------------------------
-- 3. RERANK 模型(重排,用于 RAG 精排)
-- -----------------------------------------------------------------------------

-- 3.1 BGE Reranker
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('bge-reranker-large', 'BGE Reranker Large', 'BGE', 'RERANK',
   'http://localhost:9100/rerank', 'VALUE:local', 512, true,
   'rerank,chinese,multilingual', 'ENABLED', 200,
   'BGE 重排模型,需 bge-reranker 服务,默认开启'),
  ('bge-reranker-v2-m3', 'BGE Reranker v2-M3', 'BGE', 'RERANK',
   'http://localhost:9100/rerank', 'VALUE:local', 8192, false,
   'rerank,multilingual,long-text', 'ENABLED', 201,
   'BGE v2-M3 多语言重排,8K 输入,适合长文档')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;

-- 3.2 Cohere Rerank(在线)
INSERT INTO ai_model_config
  (model_code, model_name, provider, model_type, endpoint, api_key_ref, context_window, default_model, capability_tags, status, sort_order, remark)
VALUES
  ('rerank-english-v3.0', 'Cohere Rerank English v3', 'Cohere', 'RERANK',
   'https://api.cohere.ai/v1/rerank', 'ENV:COHERE_API_KEY', 512, false,
   'rerank,english', 'ENABLED', 210,
   'Cohere 在线 Rerank 服务,英文场景精度高')
ON CONFLICT (model_code) WHERE deleted = 0 DO NOTHING;


-- -----------------------------------------------------------------------------
-- 4. 同步 PostgreSQL 序列(让后续自增 ID 不会与种子冲突)
-- -----------------------------------------------------------------------------
SELECT setval(
  pg_get_serial_sequence('ai_model_config', 'model_id'),
  GREATEST((SELECT COALESCE(MAX(model_id), 0) FROM ai_model_config), 1),
  true
);

COMMIT;

-- =============================================================================
-- 验证查询
-- =============================================================================
-- 执行成功后可运行下面三条 SQL 确认数据:
--
--   -- 总数与分组
--   SELECT model_type, COUNT(*) AS count,
--          SUM(CASE WHEN default_model THEN 1 ELSE 0 END) AS default_count
--   FROM ai_model_config
--   WHERE deleted = 0
--   GROUP BY model_type
--   ORDER BY model_type;
--
--   -- 配置完整度(找出缺 endpoint / api_key_ref 的记录)
--   SELECT model_code, model_name, provider,
--          CASE WHEN endpoint IS NULL THEN '⚠️ 缺 endpoint' ELSE '✓' END AS endpoint_status,
--          CASE WHEN api_key_ref IS NULL THEN '⚠️ 缺 key' ELSE '✓' END AS key_status
--   FROM ai_model_config
--   WHERE deleted = 0
--   ORDER BY model_type, sort_order;
--
--   -- 测试降级链路(任意 chat 请求会返回 metadata.providerCall='LOCAL_FALLBACK')
--   -- 如果想立即生效真实模型,记得重启 Spring Boot 让 ENV 变量被加载
-- =============================================================================
