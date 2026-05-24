insert into sys_dept (dept_id, parent_id, ancestors, dept_code, dept_name, leader, phone, sort_no, status, remark,
                      create_by, create_time, update_by, update_time, deleted)
values (1, 0, '0', 'ROOT', 'Techen Cloud', 'System', '13800000000', 0, 'ENABLED', 'Root department',
        0, current_timestamp, 0, current_timestamp, 0)
on conflict (dept_id) do nothing;

insert into sys_role (role_id, role_code, role_name, data_scope, status, remark,
                      create_by, create_time, update_by, update_time, deleted)
values (1, 'SYSTEM_ADMIN', 'System Administrator', 'ALL', 'ENABLED', 'Built-in administrator role',
        0, current_timestamp, 0, current_timestamp, 0)
on conflict (role_id) do nothing;

insert into sys_user (user_id, username, nickname, password, dept_id, phone, email, status, super_admin, remark,
                      create_by, create_time, update_by, update_time, deleted)
values (1, 'admin', 'Administrator',
        '{bcrypt}$2a$10$JK8mrdRGyS7.lrmDmoFRCO9muxJx5A7gWOYbP2DciCCesgd1mCjhS',
        1, '13800000000', 'admin@techen.cloud', 'ENABLED', true, 'Built-in administrator account',
        0, current_timestamp, 0, current_timestamp, 0)
on conflict (user_id) do nothing;

insert into sys_user_role (id, user_id, role_id, create_by, create_time, update_by, update_time, deleted)
values (1, 1, 1, 0, current_timestamp, 0, current_timestamp, 0)
on conflict (id) do nothing;

insert into sys_menu (menu_id, parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon,
                      sort_no, visible, keep_alive, external_link, status, remark,
                      create_by, create_time, update_by, update_time, deleted)
values
    (1, 0, 'Dashboard', 'MENU', '/dashboard', 'dashboard/index', 'dashboard:view', 'HomeFilled', 1, true, true, false, 'ENABLED', 'Dashboard',
     0, current_timestamp, 0, current_timestamp, 0),
    (10, 0, 'System', 'DIRECTORY', '/system', 'Layout', null, 'Setting', 10, true, false, false, 'ENABLED', 'System management',
     0, current_timestamp, 0, current_timestamp, 0),
    (11, 10, 'Users', 'MENU', '/system/users', 'system/user/index', 'system:user:view', 'User', 11, true, true, false, 'ENABLED', 'User management',
     0, current_timestamp, 0, current_timestamp, 0),
    (12, 10, 'Roles', 'MENU', '/system/roles', 'system/role/index', 'system:role:view', 'Avatar', 12, true, true, false, 'ENABLED', 'Role management',
     0, current_timestamp, 0, current_timestamp, 0),
    (13, 10, 'Departments', 'MENU', '/system/depts', 'system/dept/index', 'system:dept:view', 'OfficeBuilding', 13, true, true, false, 'ENABLED', 'Department management',
     0, current_timestamp, 0, current_timestamp, 0),
    (14, 10, 'Menus', 'MENU', '/system/menus', 'system/menu/index', 'system:menu:view', 'Menu', 14, true, true, false, 'ENABLED', 'Menu management',
     0, current_timestamp, 0, current_timestamp, 0),
    (111, 11, 'User Create', 'BUTTON', null, null, 'system:user:create', null, 1, true, false, false, 'ENABLED', 'Create user',
     0, current_timestamp, 0, current_timestamp, 0),
    (112, 11, 'User Update', 'BUTTON', null, null, 'system:user:update', null, 2, true, false, false, 'ENABLED', 'Update user',
     0, current_timestamp, 0, current_timestamp, 0),
    (113, 11, 'User Delete', 'BUTTON', null, null, 'system:user:delete', null, 3, true, false, false, 'ENABLED', 'Delete user',
     0, current_timestamp, 0, current_timestamp, 0),
    (121, 12, 'Role Save', 'BUTTON', null, null, 'system:role:save', null, 1, true, false, false, 'ENABLED', 'Save role',
     0, current_timestamp, 0, current_timestamp, 0),
    (131, 13, 'Dept Save', 'BUTTON', null, null, 'system:dept:save', null, 1, true, false, false, 'ENABLED', 'Save department',
     0, current_timestamp, 0, current_timestamp, 0),
    (141, 14, 'Menu Save', 'BUTTON', null, null, 'system:menu:save', null, 1, true, false, false, 'ENABLED', 'Save menu',
     0, current_timestamp, 0, current_timestamp, 0)
on conflict (menu_id) do nothing;

insert into sys_role_menu (id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
select row_number() over (), 1, menu_id, 0, current_timestamp, 0, current_timestamp, 0
from sys_menu
where deleted = 0
on conflict do nothing;
