
-- Sequences, normally created by Java-based migration

CREATE SEQUENCE "libertybans_punishment_ids" AS BIGINT;

CREATE SEQUENCE "libertybans_victim_ids" AS INT;

-- LibertyBans 0.8.x tables

CREATE TABLE "${tableprefix}zeroeight_names" (
"uuid" UUID NOT NULL,
"name" VARCHAR(16) NOT NULL,
"updated" BIGINT NOT NULL,
UNIQUE ("uuid", "name"));

CREATE TABLE "${tableprefix}zeroeight_addresses" (
"uuid" UUID NOT NULL,
"address" VARBINARY(16) NOT NULL,
"updated" BIGINT NOT NULL,
UNIQUE ("uuid", "address"));

CREATE TABLE "${tableprefix}zeroeight_punishments" (
"id" INT NOT NULL UNIQUE,
"type" VARCHAR(4) NOT NULL CHECK ("type" IN ('BAN', 'MUTE', 'WARN', 'KICK')),
"operator" UUID NOT NULL,
"reason" VARCHAR(256) NOT NULL,
"scope" VARCHAR(32) NOT NULL,
"start" BIGINT NOT NULL,
"end" BIGINT NOT NULL);

CREATE TABLE "${tableprefix}zeroeight_bans" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${tableprefix}zeroeight_punishments" ("id") ON DELETE CASCADE,
UNIQUE ("victim", "victim_type"));

CREATE TABLE "${tableprefix}zeroeight_mutes" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${tableprefix}zeroeight_punishments" ("id") ON DELETE CASCADE,
UNIQUE ("victim", "victim_type"));

CREATE TABLE "${tableprefix}zeroeight_warns" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${tableprefix}zeroeight_punishments" ("id") ON DELETE CASCADE);

CREATE TABLE "${tableprefix}zeroeight_history" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${tableprefix}zeroeight_punishments" ("id") ON DELETE CASCADE);
