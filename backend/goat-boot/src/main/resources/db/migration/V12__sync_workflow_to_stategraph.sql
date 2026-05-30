-- ============================================
-- V12: Sync ai_workflow seed data to ai_stategraph
-- ============================================

-- Insert the daily-inspection workflow into ai_stategraph if it doesn't exist
INSERT INTO ai_stategraph (graph_code, graph_name, description, version, definition_json, status, create_by, create_time, update_by, update_time, deleted)
SELECT
    w.workflow_code,
    w.workflow_name,
    w.description,
    w.version,
    w.graph_json::jsonb,
    'ACTIVE' AS status,
    w.create_by,
    w.create_time,
    w.update_by,
    w.update_time,
    w.deleted
FROM ai_workflow w
WHERE w.workflow_code = 'daily-inspection'
  AND w.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM ai_stategraph s WHERE s.graph_code = w.workflow_code AND s.deleted = 0
  );

-- Insert nodes for the daily-inspection workflow
INSERT INTO ai_state_node (graph_id, node_code, node_name, node_type, config_json, sort_order, edges_json)
SELECT
    s.graph_id,
    n.node_code,
    n.node_name,
    n.node_type,
    n.config_json::jsonb,
    n.sort_order,
    n.edges_json::jsonb
FROM ai_stategraph s
CROSS JOIN (VALUES
    ('start',   'Start',      'START',       NULL,                             1, NULL),
    ('health',  'Health Check', 'PASSTHROUGH', '{"action":"health-check"}',    2, '["summary"]'::jsonb),
    ('summary', 'Summary',    'CHAT',        '{"prompt":"Summarize results"}', 3, NULL)
) AS n(node_code, node_name, node_type, config_json, sort_order, edges_json)
WHERE s.graph_code = 'daily-inspection'
  AND s.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM ai_state_node sn WHERE sn.graph_id = s.graph_id AND sn.node_code = n.node_code AND sn.deleted = 0
  );
