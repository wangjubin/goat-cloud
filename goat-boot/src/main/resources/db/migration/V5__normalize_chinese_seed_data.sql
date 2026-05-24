update sys_dept
set dept_name = 'Techen Cloud 总部',
    leader = '系统管理员',
    remark = '根组织',
    update_by = 0,
    update_time = current_timestamp
where dept_id = 1
  and deleted = 0;

update sys_role
set role_name = '系统管理员',
    remark = '内置系统管理员角色',
    update_by = 0,
    update_time = current_timestamp
where role_id = 1
  and deleted = 0;

update sys_user
set nickname = '系统管理员',
    remark = '内置管理员账号',
    update_by = 0,
    update_time = current_timestamp
where user_id = 1
  and deleted = 0;

update sys_menu
set menu_name = values_map.menu_name,
    remark = values_map.remark,
    update_by = 0,
    update_time = current_timestamp
from (
    values
        (1, '首页', '系统首页'),
        (10, '系统管理', '系统管理'),
        (11, '用户管理', '用户管理'),
        (12, '角色管理', '角色管理'),
        (13, '组织管理', '组织管理'),
        (14, '菜单管理', '菜单管理'),
        (111, '用户新增', '新增用户'),
        (112, '用户编辑', '编辑用户'),
        (113, '用户删除', '删除用户'),
        (121, '角色保存', '新增或编辑角色'),
        (131, '组织保存', '新增或编辑组织'),
        (141, '菜单保存', '新增或编辑菜单'),
        (200, 'AI 中台', 'AI 基础能力'),
        (201, 'AI 助手', 'AI 助手'),
        (202, '模型配置', '模型配置'),
        (203, '向量配置', 'PostgreSQL 向量配置'),
        (204, '提示词管理', '提示词模板管理'),
        (205, '账单统计', '模型调用账单统计'),
        (210, 'RAG 知识库', 'RAG 知识库'),
        (211, '知识库管理', '知识库管理'),
        (212, '文档管理', '文档管理'),
        (213, '切片管理', '文档切片管理'),
        (220, 'AI MCP', 'MCP 工具管理'),
        (221, 'API Skills', 'API Skills 管理'),
        (230, 'AI 问数', 'Chat2BI 智能问数'),
        (231, '问数总览', '问数总览'),
        (232, '数据源', '问数数据源'),
        (233, '数据表', '问数数据表'),
        (234, '数据集', '问数数据集'),
        (235, '术语管理', '业务术语管理'),
        (240, 'AI 智能体', 'AI 智能体'),
        (241, '流程编排', 'Agent 工作流编排')
) as values_map(menu_id, menu_name, remark)
where sys_menu.menu_id = values_map.menu_id
  and sys_menu.deleted = 0;

update ai_model_config
set model_name = values_map.model_name,
    remark = values_map.remark,
    update_time = current_timestamp
from (
    values
        ('general-chat', '通用大模型', '用于 AI 助手、智能体和流程编排'),
        ('text-embedding', '文本向量模型', '用于 RAG 文档向量化')
) as values_map(model_code, model_name, remark)
where ai_model_config.model_code = values_map.model_code
  and ai_model_config.deleted = 0;

update ai_vector_config
set config_name = 'PostgreSQL 向量库',
    remark = '向量库使用 PostgreSQL，生产环境建议安装 pgvector 扩展',
    update_time = current_timestamp
where vector_config_id = 1
  and deleted = 0;

update ai_prompt_template
set prompt_name = '默认助手提示词',
    system_prompt = '你是企业 AI 助手，请基于上下文、工具和引用回答问题。',
    remark = 'AI 助手默认提示词',
    update_time = current_timestamp
where prompt_code = 'default-assistant'
  and deleted = 0;

update ai_knowledge_base
set knowledge_base_name = '企业知识库',
    description = '企业制度、产品文档与操作手册',
    remark = 'RAG 知识库基础样例',
    update_time = current_timestamp
where knowledge_base_code = 'enterprise-kb'
  and deleted = 0;

update ai_document
set document_name = values_map.document_name,
    remark = '知识库样例文档',
    update_time = current_timestamp
from (
    values
        ('local://samples/product-manual.pdf', '产品使用手册.pdf'),
        ('local://samples/ops-faq.md', '运维处理 FAQ.md')
) as values_map(source_uri, document_name)
where ai_document.source_uri = values_map.source_uri
  and ai_document.deleted = 0;

update ai_document_chunk
set title = values_map.title,
    content = values_map.content,
    remark = '样例切片',
    update_time = current_timestamp
from (
    values
        (1, 1, '登录与权限', '系统通过用户、角色、组织和菜单控制访问权限。'),
        (1, 2, '动态菜单', '前端登录后根据后端菜单树动态注册可访问路由。'),
        (2, 1, '故障排查', '当服务不可用时，应优先检查 PostgreSQL、Redis 和后端健康状态。')
) as values_map(document_id, chunk_index, title, content)
where ai_document_chunk.document_id = values_map.document_id
  and ai_document_chunk.chunk_index = values_map.chunk_index
  and ai_document_chunk.deleted = 0;

update ai_mcp_tool
set tool_name = '知识库检索工具',
    remark = '用于 AI 助手检索知识库',
    update_time = current_timestamp
where tool_code = 'knowledge-search'
  and deleted = 0;

update ai_api_skill
set skill_name = '系统健康检查',
    remark = 'AI 可调用的 API Skill 样例',
    update_time = current_timestamp
where skill_code = 'system-health'
  and deleted = 0;

update ai_chatbi_datasource
set datasource_name = '业务 PostgreSQL',
    remark = 'Chat2BI 默认数据源',
    update_time = current_timestamp
where datasource_code = 'postgres-main'
  and deleted = 0;

update ai_chatbi_table
set table_comment = values_map.table_comment,
    columns_json = values_map.columns_json,
    remark = '问数样例数据表',
    update_time = current_timestamp
from (
    values
        ('sys_user', '用户表', '[{"name":"username","comment":"用户名"},{"name":"dept_id","comment":"组织ID"}]'),
        ('sys_dept', '组织表', '[{"name":"dept_name","comment":"组织名称"}]')
) as values_map(table_name, table_comment, columns_json)
where ai_chatbi_table.table_name = values_map.table_name
  and ai_chatbi_table.deleted = 0;

update ai_chatbi_dataset
set dataset_name = '系统用户分析数据集',
    remark = 'Chat2BI 样例数据集',
    update_time = current_timestamp
where dataset_code = 'system-user-analysis'
  and deleted = 0;

update ai_chatbi_term
set term_name = '启用用户数',
    synonyms = '有效用户,正常用户',
    definition = '状态为启用且未删除的用户数量',
    remark = '问数业务术语样例',
    update_time = current_timestamp
where term_code = 'active-user-count'
  and deleted = 0;

update ai_agent
set agent_name = '运维助手',
    description = '面向日常自动化和复杂任务分解的 AI 智能体',
    remark = '智能体样例',
    update_time = current_timestamp
where agent_code = 'ops-assistant'
  and deleted = 0;

update ai_workflow
set workflow_name = '日常巡检流程',
    description = '通过节点编排完成系统健康检查、知识检索和结果总结',
    remark = '流程编排样例',
    update_time = current_timestamp
where workflow_code = 'daily-inspection'
  and deleted = 0;
