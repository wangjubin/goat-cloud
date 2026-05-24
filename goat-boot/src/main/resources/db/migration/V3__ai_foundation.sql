do $$
begin
    create extension if not exists vector;
exception
    when others then
        raise notice 'pgvector extension is not available yet: %', sqlerrm;
end $$;

create table if not exists ai_model_config
(
    model_id       bigserial primary key,
    model_name     varchar(100) not null,
    provider       varchar(100) not null,
    model_code     varchar(100) not null,
    model_type     varchar(50)  not null,
    endpoint       varchar(500),
    api_key_ref    varchar(255),
    context_window integer,
    default_model  boolean      not null default false,
    status         varchar(16)  not null default 'ENABLED',
    sort_order     integer      not null default 0,
    remark         varchar(500),
    create_by      bigint       not null default 0,
    create_time    timestamp    not null default current_timestamp,
    update_by      bigint       not null default 0,
    update_time    timestamp    not null default current_timestamp,
    deleted        integer      not null default 0
);

create unique index if not exists uk_ai_model_config_code on ai_model_config (model_code) where deleted = 0;

create table if not exists ai_vector_config
(
    vector_config_id    bigserial primary key,
    config_name         varchar(100) not null,
    provider            varchar(100) not null default 'POSTGRESQL',
    embedding_model     varchar(100),
    embedding_dimension integer      not null default 1536,
    distance_metric     varchar(50)  not null default 'COSINE',
    pgvector_table      varchar(100) not null default 'ai_document_chunk',
    index_type          varchar(50)  not null default 'IVFFLAT',
    chunk_size          integer      not null default 800,
    chunk_overlap       integer      not null default 120,
    status              varchar(16)  not null default 'ENABLED',
    remark              varchar(500),
    create_by           bigint       not null default 0,
    create_time         timestamp    not null default current_timestamp,
    update_by           bigint       not null default 0,
    update_time         timestamp    not null default current_timestamp,
    deleted             integer      not null default 0
);

create table if not exists ai_prompt_template
(
    prompt_id     bigserial primary key,
    prompt_code   varchar(100) not null,
    prompt_name   varchar(100) not null,
    prompt_type   varchar(50)  not null default 'ASSISTANT',
    system_prompt text,
    user_prompt   text,
    variables     text,
    version       varchar(50)  not null default '1.0.0',
    status        varchar(16)  not null default 'ENABLED',
    remark        varchar(500),
    create_by     bigint      not null default 0,
    create_time   timestamp   not null default current_timestamp,
    update_by     bigint      not null default 0,
    update_time   timestamp   not null default current_timestamp,
    deleted       integer     not null default 0
);

create unique index if not exists uk_ai_prompt_template_code on ai_prompt_template (prompt_code) where deleted = 0;

create table if not exists ai_billing_record
(
    billing_id        bigserial primary key,
    conversation_id   varchar(100),
    provider          varchar(100),
    model_code        varchar(100),
    biz_type          varchar(50),
    prompt_tokens     integer        not null default 0,
    completion_tokens integer        not null default 0,
    total_tokens      integer        not null default 0,
    cost_amount       numeric(18, 6) not null default 0,
    currency          varchar(20)    not null default 'CNY',
    request_time      timestamp      not null default current_timestamp,
    status            varchar(16)    not null default 'ENABLED',
    remark            varchar(500),
    create_by         bigint         not null default 0,
    create_time       timestamp      not null default current_timestamp,
    update_by         bigint         not null default 0,
    update_time       timestamp      not null default current_timestamp,
    deleted           integer        not null default 0
);

create table if not exists ai_knowledge_base
(
    knowledge_base_id   bigserial primary key,
    knowledge_base_code varchar(100) not null,
    knowledge_base_name varchar(100) not null,
    description         varchar(500),
    vector_config_id    bigint,
    embedding_model     varchar(100),
    embedding_dimension integer      not null default 1536,
    document_count      bigint       not null default 0,
    chunk_count         bigint       not null default 0,
    status              varchar(16)  not null default 'ENABLED',
    remark              varchar(500),
    create_by           bigint       not null default 0,
    create_time         timestamp    not null default current_timestamp,
    update_by           bigint       not null default 0,
    update_time         timestamp    not null default current_timestamp,
    deleted             integer      not null default 0
);

create unique index if not exists uk_ai_knowledge_base_code on ai_knowledge_base (knowledge_base_code) where deleted = 0;

create table if not exists ai_document
(
    document_id       bigserial primary key,
    knowledge_base_id bigint       not null,
    document_name     varchar(255) not null,
    document_type     varchar(50),
    source_uri        varchar(500),
    file_size         bigint       not null default 0,
    parse_status      varchar(50)  not null default 'PENDING',
    chunk_status      varchar(50)  not null default 'PENDING',
    metadata          text,
    status            varchar(16)  not null default 'ENABLED',
    remark            varchar(500),
    create_by         bigint       not null default 0,
    create_time       timestamp    not null default current_timestamp,
    update_by         bigint       not null default 0,
    update_time       timestamp    not null default current_timestamp,
    deleted           integer      not null default 0
);

create table if not exists ai_document_chunk
(
    chunk_id          bigserial primary key,
    knowledge_base_id bigint      not null,
    document_id       bigint      not null,
    chunk_index       integer     not null default 0,
    title             varchar(255),
    content           text        not null,
    token_count       integer     not null default 0,
    embedding_status  varchar(50) not null default 'PENDING',
    embedding_vector  text,
    metadata          text,
    status            varchar(16) not null default 'ENABLED',
    remark            varchar(500),
    create_by         bigint      not null default 0,
    create_time       timestamp   not null default current_timestamp,
    update_by         bigint      not null default 0,
    update_time       timestamp   not null default current_timestamp,
    deleted           integer     not null default 0
);

create table if not exists ai_mcp_tool
(
    mcp_tool_id    bigserial primary key,
    tool_code      varchar(100) not null,
    tool_name      varchar(100) not null,
    server_name    varchar(100),
    transport_type varchar(50)  not null default 'stdio',
    endpoint       varchar(500),
    input_schema   text,
    output_schema  text,
    status         varchar(16)  not null default 'ENABLED',
    remark         varchar(500),
    create_by      bigint      not null default 0,
    create_time    timestamp   not null default current_timestamp,
    update_by      bigint      not null default 0,
    update_time    timestamp   not null default current_timestamp,
    deleted        integer     not null default 0
);

create unique index if not exists uk_ai_mcp_tool_code on ai_mcp_tool (tool_code) where deleted = 0;

create table if not exists ai_api_skill
(
    api_skill_id    bigserial primary key,
    skill_code      varchar(100) not null,
    skill_name      varchar(100) not null,
    skill_type      varchar(50)  not null default 'REST',
    endpoint        varchar(500),
    http_method     varchar(20)  not null default 'GET',
    auth_type       varchar(50)  not null default 'NONE',
    request_schema  text,
    response_schema text,
    status          varchar(16)  not null default 'ENABLED',
    remark          varchar(500),
    create_by       bigint      not null default 0,
    create_time     timestamp   not null default current_timestamp,
    update_by       bigint      not null default 0,
    update_time     timestamp   not null default current_timestamp,
    deleted         integer     not null default 0
);

create unique index if not exists uk_ai_api_skill_code on ai_api_skill (skill_code) where deleted = 0;

create table if not exists ai_chatbi_datasource
(
    datasource_id   bigserial primary key,
    datasource_code varchar(100) not null,
    datasource_name varchar(100) not null,
    datasource_type varchar(50)  not null default 'POSTGRESQL',
    jdbc_url        varchar(500),
    username        varchar(100),
    credential_ref  varchar(255),
    status          varchar(16)  not null default 'ENABLED',
    remark          varchar(500),
    create_by       bigint      not null default 0,
    create_time     timestamp   not null default current_timestamp,
    update_by       bigint      not null default 0,
    update_time     timestamp   not null default current_timestamp,
    deleted         integer     not null default 0
);

create unique index if not exists uk_ai_chatbi_datasource_code on ai_chatbi_datasource (datasource_code) where deleted = 0;

create table if not exists ai_chatbi_table
(
    table_id      bigserial primary key,
    datasource_id bigint      not null,
    schema_name   varchar(100),
    table_name    varchar(100) not null,
    table_comment varchar(500),
    columns_json  text,
    status        varchar(16)  not null default 'ENABLED',
    remark        varchar(500),
    create_by     bigint      not null default 0,
    create_time   timestamp   not null default current_timestamp,
    update_by     bigint      not null default 0,
    update_time   timestamp   not null default current_timestamp,
    deleted       integer     not null default 0
);

create table if not exists ai_chatbi_dataset
(
    dataset_id      bigserial primary key,
    dataset_code    varchar(100) not null,
    dataset_name    varchar(100) not null,
    datasource_id   bigint,
    table_ids       text,
    semantic_model  text,
    default_filters text,
    status          varchar(16)  not null default 'ENABLED',
    remark          varchar(500),
    create_by       bigint      not null default 0,
    create_time     timestamp   not null default current_timestamp,
    update_by       bigint      not null default 0,
    update_time     timestamp   not null default current_timestamp,
    deleted         integer     not null default 0
);

create unique index if not exists uk_ai_chatbi_dataset_code on ai_chatbi_dataset (dataset_code) where deleted = 0;

create table if not exists ai_chatbi_term
(
    term_id     bigserial primary key,
    term_code   varchar(100) not null,
    term_name   varchar(100) not null,
    synonyms    varchar(500),
    definition  text,
    expression  text,
    dataset_id  bigint,
    status      varchar(16)  not null default 'ENABLED',
    remark      varchar(500),
    create_by   bigint      not null default 0,
    create_time timestamp   not null default current_timestamp,
    update_by   bigint      not null default 0,
    update_time timestamp   not null default current_timestamp,
    deleted     integer     not null default 0
);

create unique index if not exists uk_ai_chatbi_term_code on ai_chatbi_term (term_code) where deleted = 0;

create table if not exists ai_agent
(
    agent_id           bigserial primary key,
    agent_code         varchar(100) not null,
    agent_name         varchar(100) not null,
    description        varchar(500),
    model_id           bigint,
    prompt_id          bigint,
    tool_ids           text,
    knowledge_base_ids text,
    memory_config      text,
    status             varchar(16)  not null default 'ENABLED',
    remark             varchar(500),
    create_by          bigint      not null default 0,
    create_time        timestamp   not null default current_timestamp,
    update_by          bigint      not null default 0,
    update_time        timestamp   not null default current_timestamp,
    deleted            integer     not null default 0
);

create unique index if not exists uk_ai_agent_code on ai_agent (agent_code) where deleted = 0;

create table if not exists ai_workflow
(
    workflow_id   bigserial primary key,
    workflow_code varchar(100) not null,
    workflow_name varchar(100) not null,
    description   varchar(500),
    trigger_type  varchar(50)  not null default 'MANUAL',
    graph_json    text,
    version       varchar(50)  not null default '1.0.0',
    status        varchar(16)  not null default 'ENABLED',
    remark        varchar(500),
    create_by     bigint      not null default 0,
    create_time   timestamp   not null default current_timestamp,
    update_by     bigint      not null default 0,
    update_time   timestamp   not null default current_timestamp,
    deleted       integer     not null default 0
);

create unique index if not exists uk_ai_workflow_code on ai_workflow (workflow_code) where deleted = 0;

insert into ai_model_config (model_code, model_name, provider, model_type, endpoint, context_window, default_model, status, sort_order, remark)
values ('general-chat', '通用大模型', 'OpenAI Compatible', 'CHAT', null, 128000, true, 'ENABLED', 1, '用于 AI 助手、智能体和流程编排'),
       ('text-embedding', '文本向量模型', 'OpenAI Compatible', 'EMBEDDING', null, 8192, true, 'ENABLED', 2, '用于 RAG 文档向量化')
on conflict do nothing;

insert into ai_vector_config (config_name, provider, embedding_model, embedding_dimension, distance_metric, pgvector_table, index_type, chunk_size, chunk_overlap, status, remark)
select 'PostgreSQL 向量库', 'POSTGRESQL', 'text-embedding', 1536, 'COSINE', 'ai_document_chunk', 'IVFFLAT', 800, 120, 'ENABLED', '向量库使用 PostgreSQL，生产环境建议安装 pgvector 扩展'
where not exists (
    select 1
    from ai_vector_config
    where provider = 'POSTGRESQL'
      and embedding_model = 'text-embedding'
      and pgvector_table = 'ai_document_chunk'
      and deleted = 0
);

insert into ai_prompt_template (prompt_code, prompt_name, prompt_type, system_prompt, user_prompt, variables, version, status, remark)
values ('default-assistant', '默认助手提示词', 'ASSISTANT', '你是企业 AI 助手，请基于上下文、工具和引用回答问题。', '{{question}}', 'question,context,citations', '1.0.0', 'ENABLED', 'AI 助手默认提示词')
on conflict do nothing;

insert into ai_knowledge_base (knowledge_base_code, knowledge_base_name, description, vector_config_id, embedding_model, embedding_dimension, document_count, chunk_count, status, remark)
values ('enterprise-kb', '企业知识库', '企业制度、产品文档与操作手册', 1, 'text-embedding', 1536, 2, 4, 'ENABLED', 'RAG 知识库基础样例')
on conflict do nothing;

insert into ai_document (knowledge_base_id, document_name, document_type, source_uri, file_size, parse_status, chunk_status, metadata, status, remark)
select kb.knowledge_base_id, seed.document_name, seed.document_type, seed.source_uri, seed.file_size, seed.parse_status, seed.chunk_status, seed.metadata, seed.status, seed.remark
from ai_knowledge_base kb
cross join (
    values
        ('产品使用手册.pdf', 'PDF', 'local://samples/product-manual.pdf', 204800::bigint, 'SUCCESS', 'SUCCESS', '{"pages":18}', 'ENABLED', '知识库样例文档'),
        ('运维处理 FAQ.md', 'MARKDOWN', 'local://samples/ops-faq.md', 51200::bigint, 'SUCCESS', 'SUCCESS', '{"sections":12}', 'ENABLED', '知识库样例文档')
) as seed(document_name, document_type, source_uri, file_size, parse_status, chunk_status, metadata, status, remark)
where kb.knowledge_base_code = 'enterprise-kb'
  and kb.deleted = 0
  and not exists (
      select 1 from ai_document doc where doc.source_uri = seed.source_uri and doc.deleted = 0
  );

insert into ai_document_chunk (knowledge_base_id, document_id, chunk_index, title, content, token_count, embedding_status, embedding_vector, metadata, status, remark)
select doc.knowledge_base_id, doc.document_id, seed.chunk_index, seed.title, seed.content, seed.token_count, 'READY', null, '{"source":"seed"}', 'ENABLED', '样例切片'
from ai_document doc
join (
    values
        ('local://samples/product-manual.pdf', 1, '登录与权限', '系统通过用户、角色、组织和菜单控制访问权限。', 128),
        ('local://samples/product-manual.pdf', 2, '动态菜单', '前端登录后根据后端菜单树动态注册可访问路由。', 112),
        ('local://samples/ops-faq.md', 1, '故障排查', '当服务不可用时，应优先检查 PostgreSQL、Redis 和后端健康状态。', 118)
) as seed(source_uri, chunk_index, title, content, token_count)
    on doc.source_uri = seed.source_uri
where doc.deleted = 0
  and not exists (
      select 1 from ai_document_chunk chunk
      where chunk.document_id = doc.document_id
        and chunk.chunk_index = seed.chunk_index
        and chunk.deleted = 0
  );

insert into ai_mcp_tool (tool_code, tool_name, server_name, transport_type, endpoint, input_schema, output_schema, status, remark)
values ('knowledge-search', '知识库检索工具', 'techen-mcp', 'stdio', null, '{"query":"string"}', '{"chunks":"array"}', 'ENABLED', '用于 AI 助手检索知识库')
on conflict do nothing;

insert into ai_api_skill (skill_code, skill_name, skill_type, endpoint, http_method, auth_type, request_schema, response_schema, status, remark)
values ('system-health', '系统健康检查', 'REST', '/actuator/health', 'GET', 'NONE', '{}', '{"status":"string"}', 'ENABLED', 'AI 可调用的 API Skill 样例')
on conflict do nothing;

insert into ai_chatbi_datasource (datasource_code, datasource_name, datasource_type, jdbc_url, username, credential_ref, status, remark)
values ('postgres-main', '业务 PostgreSQL', 'POSTGRESQL', 'jdbc:postgresql://localhost:5432/goat_cloud', 'postgres', 'ENV:POSTGRES_PASSWORD', 'ENABLED', 'Chat2BI 默认数据源')
on conflict do nothing;

insert into ai_chatbi_table (datasource_id, schema_name, table_name, table_comment, columns_json, status, remark)
select ds.datasource_id, seed.schema_name, seed.table_name, seed.table_comment, seed.columns_json, 'ENABLED', '问数样例数据表'
from ai_chatbi_datasource ds
cross join (
    values
        ('public', 'sys_user', '用户表', '[{"name":"username","comment":"用户名"},{"name":"dept_id","comment":"组织ID"}]'),
        ('public', 'sys_dept', '组织表', '[{"name":"dept_name","comment":"组织名称"}]')
) as seed(schema_name, table_name, table_comment, columns_json)
where ds.datasource_code = 'postgres-main'
  and ds.deleted = 0
  and not exists (
      select 1 from ai_chatbi_table t
      where t.datasource_id = ds.datasource_id
        and t.schema_name = seed.schema_name
        and t.table_name = seed.table_name
        and t.deleted = 0
  );

insert into ai_chatbi_dataset (dataset_code, dataset_name, datasource_id, table_ids, semantic_model, default_filters, status, remark)
select 'system-user-analysis', '系统用户分析数据集', ds.datasource_id, '1,2', '{"joins":[{"left":"sys_user.dept_id","right":"sys_dept.dept_id"}]}', 'deleted = 0', 'ENABLED', 'Chat2BI 样例数据集'
from ai_chatbi_datasource ds
where ds.datasource_code = 'postgres-main'
  and ds.deleted = 0
on conflict do nothing;

insert into ai_chatbi_term (term_code, term_name, synonyms, definition, expression, dataset_id, status, remark)
select 'active-user-count', '启用用户数', '有效用户,正常用户', '状态为启用且未删除的用户数量', 'count(sys_user.user_id)', ds.dataset_id, 'ENABLED', '问数业务术语样例'
from ai_chatbi_dataset ds
where ds.dataset_code = 'system-user-analysis'
  and ds.deleted = 0
on conflict do nothing;

insert into ai_agent (agent_code, agent_name, description, model_id, prompt_id, tool_ids, knowledge_base_ids, memory_config, status, remark)
values ('ops-assistant', '运维助手', '面向日常自动化和复杂任务分解的 AI 智能体', 1, 1, '1', '1', '{"type":"short-term"}', 'ENABLED', '智能体样例')
on conflict do nothing;

insert into ai_workflow (workflow_code, workflow_name, description, trigger_type, graph_json, version, status, remark)
values ('daily-inspection', '日常巡检流程', '通过节点编排完成系统健康检查、知识检索和结果总结', 'MANUAL', '{"nodes":[{"id":"start","type":"start"},{"id":"health","type":"api-skill"},{"id":"summary","type":"llm"}],"edges":[["start","health"],["health","summary"]]}', '1.0.0', 'ENABLED', '流程编排样例')
on conflict do nothing;

insert into sys_menu (menu_id, parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon,
                      sort_no, visible, keep_alive, external_link, status, remark,
                      create_by, create_time, update_by, update_time, deleted)
values
    (200, 0, 'AI 中台', 'DIRECTORY', '/ai', 'Layout', null, 'MagicStick', 20, true, false, false, 'ENABLED', 'AI 基础能力', 0, current_timestamp, 0, current_timestamp, 0),
    (201, 200, 'AI 助手', 'MENU', '/ai/chat', 'ai/chat/index', 'ai:chat:view', 'ChatDotRound', 201, true, true, false, 'ENABLED', 'AI 助手', 0, current_timestamp, 0, current_timestamp, 0),
    (202, 200, '模型配置', 'MENU', '/ai/models', 'ai/model/index', 'ai:model:view', 'Cpu', 202, true, true, false, 'ENABLED', '模型配置', 0, current_timestamp, 0, current_timestamp, 0),
    (203, 200, '向量配置', 'MENU', '/ai/vectors', 'ai/vector/index', 'ai:vector:view', 'Connection', 203, true, true, false, 'ENABLED', 'PostgreSQL 向量配置', 0, current_timestamp, 0, current_timestamp, 0),
    (204, 200, '提示词管理', 'MENU', '/ai/prompts', 'ai/prompt/index', 'ai:prompt:view', 'Tickets', 204, true, true, false, 'ENABLED', '提示词模板管理', 0, current_timestamp, 0, current_timestamp, 0),
    (205, 200, '账单统计', 'MENU', '/ai/billing', 'ai/billing/index', 'ai:billing:view', 'DataLine', 205, true, true, false, 'ENABLED', '模型调用账单统计', 0, current_timestamp, 0, current_timestamp, 0),
    (210, 200, 'RAG 知识库', 'DIRECTORY', '/ai/rag', 'Layout', null, 'Collection', 210, true, false, false, 'ENABLED', 'RAG 知识库', 0, current_timestamp, 0, current_timestamp, 0),
    (211, 210, '知识库管理', 'MENU', '/ai/rag/knowledge', 'ai/knowledge/index', 'ai:rag:knowledge:view', 'Files', 211, true, true, false, 'ENABLED', '知识库管理', 0, current_timestamp, 0, current_timestamp, 0),
    (212, 210, '文档管理', 'MENU', '/ai/rag/documents', 'ai/document/index', 'ai:rag:document:view', 'Document', 212, true, true, false, 'ENABLED', '文档管理', 0, current_timestamp, 0, current_timestamp, 0),
    (213, 210, '切片管理', 'MENU', '/ai/rag/chunks', 'ai/chunk/index', 'ai:rag:chunk:view', 'Coin', 213, true, true, false, 'ENABLED', '文档切片管理', 0, current_timestamp, 0, current_timestamp, 0),
    (220, 200, 'AI MCP', 'MENU', '/ai/mcp', 'ai/mcp/index', 'ai:mcp:view', 'Tools', 220, true, true, false, 'ENABLED', 'MCP 工具管理', 0, current_timestamp, 0, current_timestamp, 0),
    (221, 200, 'API Skills', 'MENU', '/ai/skills', 'ai/api-skill/index', 'ai:skill:view', 'SetUp', 221, true, true, false, 'ENABLED', 'API Skills 管理', 0, current_timestamp, 0, current_timestamp, 0),
    (230, 200, 'AI 问数', 'DIRECTORY', '/ai/ask', 'Layout', null, 'TrendCharts', 230, true, false, false, 'ENABLED', 'Chat2BI 智能问数', 0, current_timestamp, 0, current_timestamp, 0),
    (231, 230, '问数总览', 'MENU', '/ai/ask/overview', 'ai/ask/index', 'ai:ask:view', 'DataAnalysis', 231, true, true, false, 'ENABLED', '问数总览', 0, current_timestamp, 0, current_timestamp, 0),
    (232, 230, '数据源', 'MENU', '/ai/ask/datasources', 'ai/ask/datasource/index', 'ai:ask:datasource:view', 'Coin', 232, true, true, false, 'ENABLED', '问数数据源', 0, current_timestamp, 0, current_timestamp, 0),
    (233, 230, '数据表', 'MENU', '/ai/ask/tables', 'ai/ask/table/index', 'ai:ask:table:view', 'Grid', 233, true, true, false, 'ENABLED', '问数数据表', 0, current_timestamp, 0, current_timestamp, 0),
    (234, 230, '数据集', 'MENU', '/ai/ask/datasets', 'ai/ask/dataset/index', 'ai:ask:dataset:view', 'Files', 234, true, true, false, 'ENABLED', '问数数据集', 0, current_timestamp, 0, current_timestamp, 0),
    (235, 230, '术语管理', 'MENU', '/ai/ask/terms', 'ai/ask/term/index', 'ai:ask:term:view', 'Notebook', 235, true, true, false, 'ENABLED', '业务术语管理', 0, current_timestamp, 0, current_timestamp, 0),
    (240, 200, 'AI 智能体', 'MENU', '/ai/agents', 'ai/agent/index', 'ai:agent:view', 'Avatar', 240, true, true, false, 'ENABLED', 'AI 智能体', 0, current_timestamp, 0, current_timestamp, 0),
    (241, 200, '流程编排', 'MENU', '/ai/workflows', 'ai/workflow/index', 'ai:workflow:view', 'Share', 241, true, true, false, 'ENABLED', 'Agent 工作流编排', 0, current_timestamp, 0, current_timestamp, 0)
on conflict (menu_id) do update set
    parent_id = excluded.parent_id,
    menu_name = excluded.menu_name,
    menu_type = excluded.menu_type,
    route_path = excluded.route_path,
    component_path = excluded.component_path,
    permission_code = excluded.permission_code,
    icon = excluded.icon,
    sort_no = excluded.sort_no,
    visible = excluded.visible,
    keep_alive = excluded.keep_alive,
    external_link = excluded.external_link,
    status = excluded.status,
    remark = excluded.remark,
    update_by = excluded.update_by,
    update_time = excluded.update_time,
    deleted = excluded.deleted;

select setval(
    pg_get_serial_sequence('sys_role_menu', 'id'),
    greatest(coalesce((select max(id) from sys_role_menu), 0), 1),
    true
);

insert into sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
select 1, m.menu_id, 0, current_timestamp, 0, current_timestamp, 0
from sys_menu m
where m.menu_id between 200 and 241
  and not exists (
      select 1 from sys_role_menu rm
      where rm.role_id = 1 and rm.menu_id = m.menu_id and rm.deleted = 0
  );
