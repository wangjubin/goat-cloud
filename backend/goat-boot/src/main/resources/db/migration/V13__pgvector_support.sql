-- ============================================
-- V13: Create document vectors table using JSONB
-- ============================================

-- Create document vectors table for RAG search (using JSONB instead of pgvector)
CREATE TABLE IF NOT EXISTS ai_document_vectors (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL UNIQUE,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    vector JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for lookup
CREATE INDEX IF NOT EXISTS idx_doc_vectors_kb_id ON ai_document_vectors(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_doc_vectors_doc_id ON ai_document_vectors(document_id);
CREATE INDEX IF NOT EXISTS idx_doc_vectors_chunk_id ON ai_document_vectors(chunk_id);
