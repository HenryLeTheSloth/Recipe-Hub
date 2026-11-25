-- Email tối đa 254 ký tự: principal_name 100 làm login thất bại với email dài.
alter table spring_session alter column principal_name type varchar(254);

-- Username duy nhất không phân biệt hoa/thường ("Admin" trùng "admin").
alter table users drop constraint users_username_key;
create unique index users_username_lower_key on users (lower(username));
