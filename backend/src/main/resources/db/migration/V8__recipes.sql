create table recipes (
    id               bigint generated always as identity primary key,
    author_id        bigint       not null references users (id) on delete restrict,
    category_id      bigint       references categories (id) on delete restrict,
    title            varchar(120) not null,
    slug             varchar(140) not null unique,
    summary          varchar(500),
    servings         int          not null check (servings between 1 and 50),
    prep_minutes     int          not null check (prep_minutes between 0 and 1440),
    cook_minutes     int          not null check (cook_minutes between 0 and 1440),
    difficulty       varchar(10)  not null,
    status           varchar(12)  not null,
    cover_media_id   bigint       references media (id) on delete set null,
    rejection_reason varchar(500),
    reviewed_by      bigint       references users (id) on delete set null,
    reviewed_at      timestamptz,
    published_at     timestamptz,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now()
);
create index recipes_status_published_idx on recipes (status, published_at desc);
create index recipes_author_idx on recipes (author_id);
create index recipes_category_idx on recipes (category_id);

create table recipe_tags (
    recipe_id bigint not null references recipes (id) on delete cascade,
    tag_id    bigint not null references tags (id) on delete cascade,
    primary key (recipe_id, tag_id)
);
create index recipe_tags_tag_idx on recipe_tags (tag_id);

-- unique DEFERRABLE: khi sửa, Hibernate chèn dòng mới trước khi xóa dòng cũ → kiểm tra lúc commit
create table recipe_ingredients (
    id            bigint generated always as identity primary key,
    recipe_id     bigint        not null references recipes (id) on delete cascade,
    ingredient_id bigint        not null references ingredients (id) on delete restrict,
    quantity      numeric(10,2) not null check (quantity > 0 and quantity <= 100000),
    unit          varchar(10)   not null,
    note          varchar(100),
    position      int           not null,
    created_at    timestamptz   not null default now(),
    updated_at    timestamptz   not null default now(),
    constraint ux_recipe_ingredients unique (recipe_id, ingredient_id) deferrable initially deferred
);
create index recipe_ingredients_ingredient_idx on recipe_ingredients (ingredient_id);

create table recipe_steps (
    id         bigint generated always as identity primary key,
    recipe_id  bigint        not null references recipes (id) on delete cascade,
    step_no    int           not null check (step_no >= 1),
    content    varchar(2000) not null,
    media_id   bigint        references media (id) on delete set null,
    created_at timestamptz   not null default now(),
    updated_at timestamptz   not null default now(),
    constraint ux_recipe_steps unique (recipe_id, step_no) deferrable initially deferred
);
