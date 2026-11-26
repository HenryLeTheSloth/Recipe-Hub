create table ratings (
    id         bigint generated always as identity primary key,
    user_id    bigint      not null references users (id) on delete restrict,
    recipe_id  bigint      not null references recipes (id) on delete cascade,
    stars      int         not null check (stars between 1 and 5),
    review     varchar(1000),
    hidden     boolean     not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ux_ratings_user_recipe unique (user_id, recipe_id)
);
create index ratings_recipe_idx on ratings (recipe_id);

create table favorites (
    user_id    bigint      not null references users (id) on delete cascade,
    recipe_id  bigint      not null references recipes (id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, recipe_id)
);
create index favorites_recipe_idx on favorites (recipe_id);

create table comments (
    id         bigint generated always as identity primary key,
    recipe_id  bigint        not null references recipes (id) on delete cascade,
    author_id  bigint        not null references users (id) on delete restrict,
    parent_id  bigint        references comments (id) on delete cascade,
    content    varchar(2000) not null,
    hidden     boolean       not null default false,
    created_at timestamptz   not null default now(),
    updated_at timestamptz   not null default now()
);
create index comments_recipe_idx on comments (recipe_id, created_at);
create index comments_parent_idx on comments (parent_id);
create index comments_author_idx on comments (author_id);
