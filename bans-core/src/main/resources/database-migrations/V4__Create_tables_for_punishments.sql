
-- Main punishments table

CREATE TABLE "${tableprefix}punishments" (
  "id" BIGINT NOT NULL,
  "type" SMALLINT NOT NULL,
  "operator" ${uuidtype} NOT NULL,
  "reason" CHARACTER VARYING(256) NOT NULL,
  "scope" CHARACTER VARYING(32) NOT NULL,
  "start" BIGINT NOT NULL,
  "end" BIGINT NOT NULL,
  CONSTRAINT "${tableprefix}punishment_id_uniqueness" UNIQUE ("id"),
  CONSTRAINT "${tableprefix}punishment_type_validity" CHECK ("type" >= 0 AND "type" <= 3)
)${extratableoptions};

CREATE INDEX "${tableprefix}punishment_operator_index" ON "${tableprefix}punishments" ("operator");
CREATE INDEX "${tableprefix}punishment_start_index" ON "${tableprefix}punishments" ("start");
CREATE INDEX "${tableprefix}punishment_end_index" ON "${tableprefix}punishments" ("end");

-- Victim table
-- The data in this table is referenced by the individual punishment tables

CREATE TABLE "${tableprefix}victims" (
  "id" INT NOT NULL,
  "type" SMALLINT NOT NULL,
  "uuid" ${uuidtype} NOT NULL,
  "address" ${inettype} NOT NULL,
  CONSTRAINT "${tableprefix}victim_id_uniqueness" UNIQUE ("id"),
  CONSTRAINT "${tableprefix}victim_type_validity" CHECK ("type" >= 0 AND "type" <= 2),
  CONSTRAINT "${tableprefix}victim_uniqueness" UNIQUE ("type", "uuid", "address")
)${extratableoptions};

-- Individual punishment tables
-- These are separate so that they may have different constraints

CREATE TABLE "${tableprefix}bans" (
  "id" BIGINT NOT NULL,
  "victim" INT NOT NULL,
  CONSTRAINT "${tableprefix}ban_id_uniqueness" UNIQUE ("id"),
  CONSTRAINT "${tableprefix}ban_id_validity" FOREIGN KEY ("id") REFERENCES "${tableprefix}punishments" ("id") ON DELETE CASCADE,
  CONSTRAINT "${tableprefix}ban_victim_uniqueness" UNIQUE ("victim"),
  CONSTRAINT "${tableprefix}ban_victim_validity" FOREIGN KEY ("victim") REFERENCES "${tableprefix}victims" ("id")
)${extratableoptions};

CREATE TABLE "${tableprefix}mutes" (
  "id" BIGINT NOT NULL,
  "victim" INT NOT NULL,
  CONSTRAINT "${tableprefix}mute_id_uniqueness" UNIQUE ("id"),
  CONSTRAINT "${tableprefix}mute_id_validity" FOREIGN KEY ("id") REFERENCES "${tableprefix}punishments" ("id") ON DELETE CASCADE,
  CONSTRAINT "${tableprefix}mute_victim_uniqueness" UNIQUE ("victim"),
  CONSTRAINT "${tableprefix}mute_victim_validity" FOREIGN KEY ("victim") REFERENCES "${tableprefix}victims" ("id")
)${extratableoptions};

CREATE TABLE "${tableprefix}warns" (
  "id" BIGINT NOT NULL,
  "victim" INT NOT NULL,
  CONSTRAINT "${tableprefix}warn_id_uniqueness" UNIQUE ("id"),
  CONSTRAINT "${tableprefix}warn_id_validity" FOREIGN KEY ("id") REFERENCES "${tableprefix}punishments" ("id") ON DELETE CASCADE,
  CONSTRAINT "${tableprefix}warn_victim_validity" FOREIGN KEY ("victim") REFERENCES "${tableprefix}victims" ("id")
)${extratableoptions};

CREATE INDEX "${tableprefix}warn_victim_index" ON "${tableprefix}warns" ("victim");

CREATE TABLE "${tableprefix}history" (
  "id" BIGINT NOT NULL,
  "victim" INT NOT NULL,
  CONSTRAINT "${tableprefix}history_id_uniqueness" UNIQUE ("id"),
  CONSTRAINT "${tableprefix}history_id_validity" FOREIGN KEY ("id") REFERENCES "${tableprefix}punishments" ("id") ON DELETE CASCADE,
  CONSTRAINT "${tableprefix}history_victim_validity" FOREIGN KEY ("victim") REFERENCES "${tableprefix}victims" ("id")
)${extratableoptions};

CREATE INDEX "${tableprefix}history_victim_index" ON "${tableprefix}history" ("victim");

CREATE TABLE "${tableprefix}messages" (
  "message" ${arbitrarybinarytype} NOT NULL,
  "time" BIGINT NOT NULL
);

CREATE INDEX "${tableprefix}messages_time_index" ON "${tableprefix}messages" ("time");
