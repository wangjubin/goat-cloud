create table if not exists sys_dept
(
    dept_id      bigserial primary key,
    parent_id    bigint      not null default 0,
    ancestors    varchar(500) not null default '0',
    dept_code    varchar(64) not null,
    dept_name    varchar(100) not null,
    leader       varchar(64),
    phone        varchar(32),
    sort_no      integer     not null default 0,
    status       varchar(16) not null default 'ENABLED',
    remark       varchar(255),
    create_by    bigint      not null default 0,
    create_time  timestamp   not null default current_timestamp,
    update_by    bigint      not null default 0,
    update_time  timestamp   not null default current_timestamp,
    deleted      integer     not null default 0
);

create table if not exists sys_role
(
    role_id      bigserial primary key,
    role_code    varchar(64) not null,
    role_name    varchar(100) not null,
    data_scope   varchar(32) not null default 'SELF',
    status       varchar(16) not null default 'ENABLED',
    remark       varchar(255),
    create_by    bigint      not null default 0,
    create_time  timestamp   not null default current_timestamp,
    update_by    bigint      not null default 0,
    update_time  timestamp   not null default current_timestamp,
    deleted      integer     not null default 0
);

create table if not exists sys_user
(
    user_id      bigserial primary key,
    username     varchar(64) not null,
    nickname     varchar(100) not null,
    password     varchar(255) not null,
    dept_id      bigint      not null,
    phone        varchar(32),
    email        varchar(128),
    status       varchar(16) not null default 'ENABLED',
    super_admin  boolean     not null default false,
    remark       varchar(255),
    create_by    bigint      not null default 0,
    create_time  timestamp   not null default current_timestamp,
    update_by    bigint      not null default 0,
    update_time  timestamp   not null default current_timestamp,
    deleted      integer     not null default 0
);

create table if not exists sys_menu
(
    menu_id         bigserial primary key,
    parent_id       bigint      not null default 0,
    menu_name       varchar(100) not null,
    menu_type       varchar(32) not null,
    route_path      varchar(255),
    component_path  varchar(255),
    permission_code varchar(128),
    icon            varchar(64),
    sort_no         integer     not null default 0,
    visible         boolean     not null default true,
    keep_alive      boolean     not null default false,
    external_link   boolean     not null default false,
    status          varchar(16) not null default 'ENABLED',
    remark          varchar(255),
    create_by       bigint      not null default 0,
    create_time     timestamp   not null default current_timestamp,
    update_by       bigint      not null default 0,
    update_time     timestamp   not null default current_timestamp,
    deleted         integer     not null default 0
);

create table if not exists sys_user_role
(
    id           bigserial primary key,
    user_id      bigint not null,
    role_id      bigint not null,
    create_by    bigint    not null default 0,
    create_time  timestamp not null default current_timestamp,
    update_by    bigint    not null default 0,
    update_time  timestamp not null default current_timestamp,
    deleted      integer   not null default 0
);

create table if not exists sys_role_menu
(
    id           bigserial primary key,
    role_id      bigint not null,
    menu_id      bigint not null,
    create_by    bigint    not null default 0,
    create_time  timestamp not null default current_timestamp,
    update_by    bigint    not null default 0,
    update_time  timestamp not null default current_timestamp,
    deleted      integer   not null default 0
);

create table if not exists sys_role_dept
(
    id           bigserial primary key,
    role_id      bigint not null,
    dept_id      bigint not null,
    create_by    bigint    not null default 0,
    create_time  timestamp not null default current_timestamp,
    update_by    bigint    not null default 0,
    update_time  timestamp not null default current_timestamp,
    deleted      integer   not null default 0
);

create table if not exists sys_login_log
(
    log_id       bigserial primary key,
    username     varchar(64) not null,
    success      boolean     not null,
    ip_address   varchar(64),
    user_agent   varchar(500),
    message      varchar(255),
    login_time   timestamp   not null default current_timestamp
);

create table if not exists sys_operation_log
(
    log_id         bigserial primary key,
    module         varchar(64),
    operation      varchar(128),
    request_method varchar(16),
    request_uri    varchar(255),
    operator_id    bigint,
    operator_name  varchar(64),
    success        boolean,
    message        varchar(255),
    create_time    timestamp not null default current_timestamp
);

create unique index if not exists uk_sys_user_username on sys_user (username) where deleted = 0;
create unique index if not exists uk_sys_role_code on sys_role (role_code) where deleted = 0;
create unique index if not exists uk_sys_dept_code on sys_dept (dept_code) where deleted = 0;
