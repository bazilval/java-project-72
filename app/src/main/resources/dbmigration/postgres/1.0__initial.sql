-- apply changes
create table url (
  id                            bigint generated by default as identity not null,
  created_at                    timestamptz not null,
  name                          varchar(255),
  constraint pk_url primary key (id)
);

create table url_check (
  id                            bigint generated by default as identity not null,
  status_code                   integer not null,
  url_id                        bigint not null,
  created_at                    timestamptz not null,
  h1                            varchar(255),
  title                         varchar(255),
  description                   text,
  constraint pk_url_check primary key (id)
);

-- foreign keys and indices
create index ix_url_check_url_id on url_check (url_id);
alter table url_check add constraint fk_url_check_url_id foreign key (url_id) references url (id) on delete cascade on update restrict;

