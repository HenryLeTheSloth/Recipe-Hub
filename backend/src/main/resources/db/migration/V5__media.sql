create table media (
    id           bigint generated always as identity primary key,
    owner_id     bigint       not null references users (id) on delete cascade,
    storage_key  varchar(64)  not null unique,
    content_type varchar(30)  not null,
    size_bytes   bigint       not null,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now()
);
create index media_owner_idx on media (owner_id);

alter table users add column avatar_media_id bigint references media (id) on delete set null;
