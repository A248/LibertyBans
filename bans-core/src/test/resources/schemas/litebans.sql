
CREATE TABLE litebans_bans (
  id BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY UNIQUE,
  uuid CHARACTER VARYING(36),
  ip CHARACTER VARYING(45),
  reason CHARACTER VARYING(2048) NOT NULL,
  banned_by_uuid CHARACTER VARYING(36),
  banned_by_name CHARACTER VARYING(128),
  removed_by_uuid CHARACTER VARYING(36),
  removed_by_name CHARACTER VARYING(128),
  removed_by_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  time BIGINT NOT NULL,
  until BIGINT NOT NULL,
  server_scope CHARACTER VARYING(32),
  server_origin CHARACTER VARYING(32),
  silent BIT NOT NULL,
  ipban BIT NOT NULL,
  ipban_wildcard BIT NOT NULL,
  active BIT NOT NULL
);

CREATE TABLE litebans_mutes (
  id BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY UNIQUE,
  uuid CHARACTER VARYING(36),
  ip CHARACTER VARYING(45),
  reason CHARACTER VARYING(2048) NOT NULL,
  banned_by_uuid CHARACTER VARYING(36),
  banned_by_name CHARACTER VARYING(128),
  removed_by_uuid CHARACTER VARYING(36),
  removed_by_name CHARACTER VARYING(128),
  removed_by_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  time BIGINT NOT NULL,
  until BIGINT NOT NULL,
  server_scope CHARACTER VARYING(32),
  server_origin CHARACTER VARYING(32),
  silent BIT NOT NULL,
  ipban BIT NOT NULL,
  ipban_wildcard BIT NOT NULL,
  active BIT NOT NULL
);

CREATE TABLE litebans_warnings (
  id BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY UNIQUE,
  uuid CHARACTER VARYING(36),
  ip CHARACTER VARYING(45),
  reason CHARACTER VARYING(2048) NOT NULL,
  banned_by_uuid CHARACTER VARYING(36),
  banned_by_name CHARACTER VARYING(128),
  removed_by_uuid CHARACTER VARYING(36),
  removed_by_name CHARACTER VARYING(128),
  removed_by_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  time BIGINT NOT NULL,
  until BIGINT NOT NULL,
  server_scope CHARACTER VARYING(32),
  server_origin CHARACTER VARYING(32),
  silent BIT NOT NULL,
  ipban BIT NOT NULL,
  ipban_wildcard BIT NOT NULL,
  active BIT NOT NULL,
  warned BIT NOT NULL
);

CREATE TABLE litebans_kicks (
  id BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY UNIQUE,
  uuid CHARACTER VARYING(36),
  ip CHARACTER VARYING(45),
  reason CHARACTER VARYING(2048) NOT NULL,
  banned_by_uuid CHARACTER VARYING(36),
  banned_by_name CHARACTER VARYING(128),
  time BIGINT,
  until BIGINT,
  server_scope CHARACTER VARYING(32),
  server_origin CHARACTER VARYING(32),
  silent BIT NOT NULL,
  ipban BIT NOT NULL,
  ipban_wildcard BIT NOT NULL,
  active BIT NOT NULL
);

CREATE TABLE litebans_history (
  id BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY UNIQUE,
  date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  name CHARACTER VARYING(16) NOT NULL,
  uuid CHARACTER VARYING(36) NOT NULL,
  ip CHARACTER VARYING(45) NOT NULL
);
