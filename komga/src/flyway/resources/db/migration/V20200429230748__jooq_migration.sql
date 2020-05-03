-- set default values for created_date / last_modified_date
alter table user
    alter column CREATED_DATE set default now();
alter table user
    alter column LAST_MODIFIED_DATE set default now();

alter table library
    alter column CREATED_DATE set default now();
alter table library
    alter column LAST_MODIFIED_DATE set default now();

alter table book
    alter column CREATED_DATE set default now();
alter table book
    alter column LAST_MODIFIED_DATE set default now();

alter table book_metadata
    alter column CREATED_DATE set default now();
alter table book_metadata
    alter column LAST_MODIFIED_DATE set default now();

alter table media
    alter column CREATED_DATE set default now();
alter table media
    alter column LAST_MODIFIED_DATE set default now();

alter table series
    alter column CREATED_DATE set default now();
alter table series
    alter column LAST_MODIFIED_DATE set default now();

alter table series_metadata
    alter column CREATED_DATE set default now();
alter table series_metadata
    alter column LAST_MODIFIED_DATE set default now();

-- replace USER_ROLE table by boolean value per role in USER table
alter table user
    add role_admin boolean default false;
update user u set role_admin = exists(select roles from user_role ur where ur.roles like 'ADMIN' and ur.user_id = u.id);
drop table user_role;
