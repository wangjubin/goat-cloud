-- ============================================
-- V11: ChatBI model config and datasource import support
-- ============================================

-- 1. ChatBI datasource: add default model for NL2SQL
ALTER TABLE ai_chatbi_datasource ADD COLUMN IF NOT EXISTS model_id BIGINT;
COMMENT ON COLUMN ai_chatbi_datasource.model_id IS 'NL2SQL default model ID';

-- 2. ChatBI datasource: add JDBC import fields
ALTER TABLE ai_chatbi_datasource ADD COLUMN IF NOT EXISTS driver_class_name VARCHAR(255) DEFAULT 'org.postgresql.Driver';
COMMENT ON COLUMN ai_chatbi_datasource.driver_class_name IS 'JDBC driver class name';

ALTER TABLE ai_chatbi_datasource ADD COLUMN IF NOT EXISTS password_encrypted VARCHAR(512);
COMMENT ON COLUMN ai_chatbi_datasource.password_encrypted IS 'Encrypted JDBC password';

-- 3. State graph: add default model
ALTER TABLE ai_stategraph ADD COLUMN IF NOT EXISTS default_model_id BIGINT;
COMMENT ON COLUMN ai_stategraph.default_model_id IS 'Workflow default model ID';

-- 4. Resync sequences
DO $$
DECLARE
    max_id BIGINT;
    seq_name TEXT;
BEGIN
    SELECT MAX(datasource_id) INTO max_id FROM ai_chatbi_datasource WHERE deleted = 0;
    seq_name := pg_get_serial_sequence('ai_chatbi_datasource', 'datasource_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;
END $$;
