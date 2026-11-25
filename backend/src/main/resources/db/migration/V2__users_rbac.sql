create table users (
    id                bigint generated always as identity primary key,
    email             varchar(254) not null unique,
    username          varchar(30)  not null unique,
    password_hash     varchar(100) not null,
    display_name      varchar(50)  not null,
    email_verified_at timestamptz,
    locked            boolean      not null default false,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);

create table roles (
    id          bigint generated always as identity primary key,
    code        varchar(31)  not null unique,
    name        varchar(50)  not null,
    description varchar(200),
    system      boolean      not null default false,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);

create table permissions (
    id          bigint generated always as identity primary key,
    code        varchar(50)  not null unique,
    description varchar(200),
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);

create table user_roles (
    user_id bigint not null references users (id) on delete cascade,
    role_id bigint not null references roles (id) on delete restrict,
    primary key (user_id, role_id)
);
create index user_roles_role_idx on user_roles (role_id);

create table role_permissions (
    role_id       bigint not null references roles (id) on delete cascade,
    permission_id bigint not null references permissions (id) on delete restrict,
    primary key (role_id, permission_id)
);

create table user_tokens (
    id         bigint generated always as identity primary key,
    user_id    bigint       not null references users (id) on delete cascade,
    type       varchar(20)  not null,
    token_hash varchar(64)  not null unique,
    expires_at timestamptz  not null,
    used_at    timestamptz,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);
create index user_tokens_user_idx on user_tokens (user_id);
