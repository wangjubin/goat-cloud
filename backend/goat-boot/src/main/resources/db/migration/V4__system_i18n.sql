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
        (141, '菜单保存', '新增或编辑菜单')
) as values_map(menu_id, menu_name, remark)
where sys_menu.menu_id = values_map.menu_id
  and sys_menu.deleted = 0;
