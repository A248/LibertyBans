CREATE TABLE litebans_bans (
  id bigint(20) IDENTITY PRIMARY KEY,
  uuid varchar(36),
  ip varchar(45),
  reason varchar(2048) NOT NULL,
  banned_by_uuid varchar(36),
  banned_by_name varchar(128),
  removed_by_uuid varchar(36),
  removed_by_name varchar(128),
  removed_by_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  time bigint(20) NOT NULL,
  until bigint(20) NOT NULL,
  server_scope varchar(32),
  server_origin varchar(32),
  silent bit(1) NOT NULL,
  ipban bit(1) NOT NULL,
  ipban_wildcard bit(1) NOT NULL,
  active bit(1) NOT NULL
);

CREATE TABLE litebans_mutes (
  id bigint(20) IDENTITY PRIMARY KEY,
  uuid varchar(36),
  ip varchar(45),
  reason varchar(2048) NOT NULL,
  banned_by_uuid varchar(36),
  banned_by_name varchar(128),
  removed_by_uuid varchar(36),
  removed_by_name varchar(128),
  removed_by_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  time bigint(20) NOT NULL,
  until bigint(20) NOT NULL,
  server_scope varchar(32),
  server_origin varchar(32),
  silent bit(1) NOT NULL,
  ipban bit(1) NOT NULL,
  ipban_wildcard bit(1) NOT NULL,
  active bit(1) NOT NULL
);

CREATE TABLE litebans_warnings (
  id bigint(20) IDENTITY PRIMARY KEY,
  uuid varchar(36),
  ip varchar(45),
  reason varchar(2048) NOT NULL,
  banned_by_uuid varchar(36),
  banned_by_name varchar(128),
  removed_by_uuid varchar(36),
  removed_by_name varchar(128),
  removed_by_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  time bigint(20) NOT NULL,
  until bigint(20) NOT NULL,
  server_scope varchar(32),
  server_origin varchar(32),
  silent bit(1) NOT NULL,
  ipban bit(1) NOT NULL,
  ipban_wildcard bit(1) NOT NULL,
  active bit(1) NOT NULL,
  warned bit(1) NOT NULL
);

CREATE TABLE litebans_kicks (
  id bigint(20) IDENTITY PRIMARY KEY,
  uuid varchar(36),
  ip varchar(45),
  reason varchar(2048) NOT NULL,
  banned_by_uuid varchar(36),
  banned_by_name varchar(128),
  time bigint(20),
  until bigint(20),
  server_scope varchar(32),
  server_origin varchar(32),
  silent bit(1) NOT NULL,
  ipban bit(1) NOT NULL,
  ipban_wildcard bit(1) NOT NULL,
  active bit(1) NOT NULL
);