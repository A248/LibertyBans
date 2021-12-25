
CREATE TABLE "${zeroeighttableprefix}revision" (
"constant" VARCHAR(8) NOT NULL UNIQUE,
"major" INT NOT NULL,
"minor" INT NOT NULL,
CHECK ("constant" = 'Constant'));

INSERT INTO "${zeroeighttableprefix}revision" ("constant", "major", "minor")
VALUES ('Constant', 2, 0);

CREATE TABLE "${zeroeighttableprefix}names" (
"uuid" BINARY(16) NOT NULL,
"name" VARCHAR(16) NOT NULL,
"updated" BIGINT NOT NULL,
UNIQUE ("uuid", "name"));

CREATE TABLE "${zeroeighttableprefix}addresses" (
"uuid" BINARY(16) NOT NULL,
"address" VARBINARY(16) NOT NULL,
"updated" BIGINT NOT NULL,
UNIQUE ("uuid", "address"));

CREATE TABLE "${zeroeighttableprefix}punishments" (
"id" INT NOT NULL UNIQUE,
"type" VARCHAR(4) NOT NULL CHECK ("type" IN ('BAN', 'MUTE', 'WARN', 'KICK')),
"operator" BINARY(16) NOT NULL,
"reason" VARCHAR(256) NOT NULL,
"scope" VARCHAR(32) NOT NULL,
"start" BIGINT NOT NULL,
"end" BIGINT NOT NULL);

CREATE TABLE "${zeroeighttableprefix}bans" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${zeroeighttableprefix}punishments" ("id") ON DELETE CASCADE,
UNIQUE ("victim", "victim_type"));

CREATE TABLE "${zeroeighttableprefix}mutes" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${zeroeighttableprefix}punishments" ("id") ON DELETE CASCADE,
UNIQUE ("victim", "victim_type"));

CREATE TABLE "${zeroeighttableprefix}warns" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${zeroeighttableprefix}punishments" ("id") ON DELETE CASCADE);

CREATE TABLE "${zeroeighttableprefix}history" (
"id" INT NOT NULL UNIQUE,
"victim" VARBINARY(16) NOT NULL,
"victim_type" VARCHAR(7) NOT NULL CHECK ("victim_type" IN ('PLAYER', 'ADDRESS')),
FOREIGN KEY ("id") REFERENCES "${zeroeighttableprefix}punishments" ("id") ON DELETE CASCADE);
