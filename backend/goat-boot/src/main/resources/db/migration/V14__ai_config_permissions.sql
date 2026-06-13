-- Add ai:config:save and ai:config:delete permissions for BaseAiCrudController
insert into sys_menu (menu_id, parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon,
                      sort_no, visible, keep_alive, external_link, status, remark,
                      create_by, create_time, update_by, update_time, deleted)
values
    (299, 200, 'AI 配置-保存', 'BUTTON', null, null, 'ai:config:save', null, 299, false, false, false, 'ENABLED', 'AI 配置保存权限', 0, current_timestamp, 0, current_timestamp, 0),
    (298, 200, 'AI 配置-删除', 'BUTTON', null, null, 'ai:config:delete', null, 298, false, false, false, 'ENABLED', 'AI 配置删除权限', 0, current_timestamp, 0, current_timestamp, 0),
    (132, 13, 'Dept Delete', 'BUTTON', null, null, 'system:dept:delete', null, 2, true, false, false, 'ENABLED', 'Delete department', 0, current_timestamp, 0, current_timestamp, 0)
on conflict (menu_id) do update set
    permission_code = excluded.permission_code,
    update_time = excluded.update_time;

-- Grant these permissions to admin role (role_id = 1)
insert into sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
select 1, m.menu_id, 0, current_timestamp, 0, current_timestamp, 0
from sys_menu m
where m.menu_id in (132, 298, 299)
  and not exists (
      select 1 from sys_role_menu rm
      where rm.role_id = 1 and rm.menu_id = m.menu_id and rm.deleted = 0
  );
