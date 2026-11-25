create table categories (
    id         bigint generated always as identity primary key,
    name       varchar(50) not null,
    slug       varchar(80) not null unique,
    sort_order int         not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create unique index ux_categories_name on categories (lower(name));

create table tags (
    id         bigint generated always as identity primary key,
    name       varchar(50) not null,
    slug       varchar(80) not null unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create unique index ux_tags_name on tags (lower(name));

create table ingredients (
    id               bigint generated always as identity primary key,
    name             varchar(60) not null,
    slug             varchar(80) not null unique,
    ingredient_group varchar(20) not null,
    emoji            varchar(16),
    status           varchar(20) not null,
    created_by       bigint references users (id) on delete set null,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);
create unique index ux_ingredients_name on ingredients (lower(name));
create index ingredients_status_group_idx on ingredients (status, ingredient_group);
