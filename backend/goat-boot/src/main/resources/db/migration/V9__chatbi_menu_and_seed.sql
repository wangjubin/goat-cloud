-- ============================================
-- V9: ChatBI StateGraph Menus & Seed Data
-- ============================================

-- 1. 添加 ChatBI 对话菜单
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, keep_alive, external_link, status, remark, create_by, create_time, update_by, update_time, deleted)
VALUES
    (236, 230, '智能问数', 'MENU', '/ai/ask/chat', 'ai/ask/chat/index', 'ai:ask:chat:view', 'ChatDotRound', 236, true, true, false, 'ENABLED', 'ChatBI 智能问数对话', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (237, 230, '流程编排', 'MENU', '/ai/ask/workflow', 'ai/ask/workflow/index', 'ai:ask:workflow:view', 'SetUp', 237, true, true, false, 'ENABLED', '工作流编排管理', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0)
ON CONFLICT (menu_id) DO UPDATE SET
  menu_name = EXCLUDED.menu_name,
  route_path = EXCLUDED.route_path,
  component_path = EXCLUDED.component_path,
  permission_code = EXCLUDED.permission_code,
  sort_no = EXCLUDED.sort_no,
  status = EXCLUDED.status;

-- 2. 为 admin 角色授权新菜单
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, 236, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 236);
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, 237, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 237);

-- 3. 创建默认 StateGraph（chatbi_default）
INSERT INTO ai_stategraph (graph_code, graph_name, description, version, definition_json, config_json, status, create_by, create_time, update_by, update_time, deleted)
VALUES (
    'chatbi_default',
    'ChatBI 默认问数流程',
    '标准智能问数工作流：意图识别 → Schema召回 → NL2SQL → 人工确认 → SQL执行 → 报告生成',
    '1.0.0',
    '{"nodes":["intent_recognition","schema_recall","nl2sql","human_feedback","sql_execution","report_generation"],"edges":[{"from":"intent_recognition","to":"schema_recall"},{"from":"schema_recall","to":"nl2sql"},{"from":"nl2sql","to":"human_feedback"},{"from":"human_feedback","to":"sql_execution"},{"from":"sql_execution","to":"report_generation"}]}',
    '{"timeoutMs":60000,"maxRetries":2}',
    'ACTIVE',
    0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0
)
ON CONFLICT (graph_code) DO UPDATE SET
  graph_name = EXCLUDED.graph_name,
  status = EXCLUDED.status,
  definition_json = EXCLUDED.definition_json;

-- 4. 创建默认 StateGraph 节点
INSERT INTO ai_state_node (graph_id, node_code, node_name, node_type, config_json, sort_order, timeout_ms, create_by, create_time, update_by, update_time, deleted)
SELECT sg.graph_id, node.node_code, node.node_name, node.node_type, node.config_json, node.sort_order, node.timeout_ms, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0
FROM ai_stategraph sg,
     (VALUES
        ('intent_recognition', '意图识别', 'INTENT_RECOGNITION', NULL::jsonb, 1, 30000),
        ('schema_recall', 'Schema召回', 'SCHEMA_RECALL', NULL::jsonb, 2, 30000),
        ('nl2sql', 'NL2SQL生成', 'NL2SQL', '{"modelType":"CHAT"}'::jsonb, 3, 60000),
        ('human_feedback', '人工确认', 'HUMAN_FEEDBACK', NULL::jsonb, 4, 300000),
        ('sql_execution', 'SQL执行', 'SQL_EXECUTION', NULL::jsonb, 5, 30000),
        ('report_generation', '报告生成', 'REPORT_GENERATION', NULL::jsonb, 6, 60000)
     ) AS node(node_code, node_name, node_type, config_json, sort_order, timeout_ms)
WHERE sg.graph_code = 'chatbi_default'
ON CONFLICT (graph_id, node_code) DO UPDATE SET
  node_name = EXCLUDED.node_name,
  node_type = EXCLUDED.node_type,
  sort_order = EXCLUDED.sort_order;

-- 5. 创建默认意图配置
INSERT INTO ai_intent_config (intent_code, intent_name, description, prompt_template, examples_json, threshold_score, fallback_intent, status, create_by, create_time, update_by, update_time, deleted)
VALUES
    ('DATA_QUERY', '数据查询', '用户想查询某个数据指标', '判断用户是否想查询数据', '{"examples":["多少","数量","统计","查询","总数","平均","count"]}', 0.75, 'DATA_QUERY', 'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    ('TREND_ANALYSIS', '趋势分析', '用户想分析数据趋势', '判断用户是否想分析趋势变化', '{"examples":["趋势","变化","增长","下降","环比","同比","trend"]}', 0.75, 'DATA_QUERY', 'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    ('DATA_COMPARE', '数据对比', '用户想对比两组或多组数据', '判断用户是否想做数据对比', '{"examples":["对比","比较","差异","vs","compare","哪个更多"]}', 0.75, 'DATA_QUERY', 'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    ('REPORT_GENERATION', '报表生成', '用户想生成报表或图表', '判断用户是否想生成报表', '{"examples":["报表","图表","可视化","报告","report","chart","dashboard"]}', 0.75, 'DATA_QUERY', 'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    ('ROOT_CAUSE', '根因分析', '用户想了解数据变化的原因', '判断用户是否想做根因分析', '{"examples":["为什么","原因","为什么下降","why","root cause"]}', 0.75, 'DATA_QUERY', 'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0)
ON CONFLICT (intent_code) DO UPDATE SET
  intent_name = EXCLUDED.intent_name,
  prompt_template = EXCLUDED.prompt_template,
  status = EXCLUDED.status;

-- 6. 创建默认报表模板
INSERT INTO ai_report_template (template_code, template_name, description, chart_type, template_json, data_mapping_json, status, create_by, create_time, update_by, update_time, deleted)
VALUES
    ('bar_default', '默认柱状图', '标准柱状图模板', 'bar',
     '{"title":{"text":"数据统计"},"tooltip":{"trigger":"axis"},"xAxis":{"type":"category"},"yAxis":{"type":"value"},"series":[{"type":"bar"}]}',
     '{"xAxis":"categories","series":"values"}',
     'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    ('line_trend', '趋势折线图', '时间序列趋势分析模板', 'line',
     '{"title":{"text":"趋势分析"},"tooltip":{"trigger":"axis"},"xAxis":{"type":"category","boundaryGap":false},"yAxis":{"type":"value"},"series":[{"type":"line","smooth":true}]}',
     '{"xAxis":"dates","series":"values"}',
     'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    ('pie_distribution', '分布饼图', '数据分布占比模板', 'pie',
     '{"title":{"text":"数据分布"},"tooltip":{"trigger":"item"},"series":[{"type":"pie","radius":"60%"}]}',
     '{"nameField":"name","valueField":"value"}',
     'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0)
ON CONFLICT (template_code) DO UPDATE SET
  template_name = EXCLUDED.template_name,
  template_json = EXCLUDED.template_json,
  status = EXCLUDED.status;

-- 7. Resync PostgreSQL sequences
DO $$
DECLARE
    max_id BIGINT;
    seq_name TEXT;
BEGIN
    -- sys_menu
    SELECT MAX(menu_id) INTO max_id FROM sys_menu;
    seq_name := pg_get_serial_sequence('sys_menu', 'menu_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;

    -- ai_stategraph
    SELECT MAX(graph_id) INTO max_id FROM ai_stategraph WHERE deleted = 0;
    seq_name := pg_get_serial_sequence('ai_stategraph', 'graph_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;

    -- ai_state_node
    SELECT MAX(node_id) INTO max_id FROM ai_state_node WHERE deleted = 0;
    seq_name := pg_get_serial_sequence('ai_state_node', 'node_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;

    -- ai_intent_config
    SELECT MAX(config_id) INTO max_id FROM ai_intent_config WHERE deleted = 0;
    seq_name := pg_get_serial_sequence('ai_intent_config', 'config_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;

    -- ai_report_template
    SELECT MAX(template_id) INTO max_id FROM ai_report_template WHERE deleted = 0;
    seq_name := pg_get_serial_sequence('ai_report_template', 'template_id');
    IF seq_name IS NOT NULL AND max_id IS NOT NULL THEN
        PERFORM setval(seq_name, max_id + 1);
    END IF;
END $$;