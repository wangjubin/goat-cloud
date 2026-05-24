-- ============================================
-- V10: Workflow DAG orchestration support
-- ============================================

-- 1. Add edges_json column to ai_state_node
ALTER TABLE ai_state_node ADD COLUMN IF NOT EXISTS edges_json JSONB;

COMMENT ON COLUMN ai_state_node.edges_json IS 'Node edge definitions: {"outgoing":[{"to":"node_code","condition":"expr"}],"incoming":["node_code"]}';

-- 2. Add workflow-specific node types: GATEWAY_AND, GATEWAY_OR, GATEWAY_XOR, SUBGRAPH, START, END
-- (These are logical types, no schema change needed - they are distinguished by node_type field)

-- 3. Add graph_type column to ai_stategraph
ALTER TABLE ai_stategraph ADD COLUMN IF NOT EXISTS graph_type VARCHAR(32) DEFAULT 'SEQUENTIAL';

COMMENT ON COLUMN ai_stategraph.graph_type IS 'Graph execution type: SEQUENTIAL, DAG, PARALLEL';

-- 4. Add workflow_id column to ai_stategraph for subgraph references
ALTER TABLE ai_stategraph ADD COLUMN IF NOT EXISTS parent_graph_id BIGINT;

COMMENT ON COLUMN ai_stategraph.parent_graph_id IS 'Parent graph ID for subgraph embedding';

-- 5. Resync sequences
DO $$
DECLARE
    max_id BIGINT;
    seq_name TEXT;
BEGIN
    SELECT MAX(graph_id) INTO max_id FROM ai_stategraph WHERE deleted = 0;
    seq_name := pg_get_serial_sequence('ai_stategraph', 'graph_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;

    SELECT MAX(node_id) INTO max_id FROM ai_state_node WHERE deleted = 0;
    seq_name := pg_get_serial_sequence('ai_state_node', 'node_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;
END $$;