
-- The LibertyBans schema
-- These files do not change. Instead, new migrations are created in this directory. The migrations run in order.
-- Please see the wiki for up-to-date schema documentation

CREATE TABLE "${tableprefix}revision" (
  "constant" CHARACTER VARYING(8) NOT NULL UNIQUE CHECK ("constant" = 'Constant'),
  "major" INT NOT NULL,
  "minor" INT NOT NULL
)${extratableoptions};

INSERT INTO "${tableprefix}revision" VALUES ('Constant', 0, 0);

-- Tables

CREATE TABLE "${tableprefix}names" (
  "uuid" ${uuidtype} NOT NULL,
  "name" CHARACTER VARYING(16) NOT NULL,
  "lower_name" CHARACTER VARYING(16) GENERATED ALWAYS AS (LOWER("name"))${generatedcolumnsuffix},
  "updated" BIGINT NOT NULL,
  CONSTRAINT "${tableprefix}uuid_name_uniqueness" UNIQUE ("uuid", "name")
)${extratableoptions};

CREATE INDEX "${tableprefix}name_index" ON "${tableprefix}names" ("lower_name");

CREATE TABLE "${tableprefix}addresses" (
  "uuid" ${uuidtype} NOT NULL,
  "address" ${inettype} NOT NULL,
  "updated" BIGINT NOT NULL,
CONSTRAINT "${tableprefix}uuid_address_uniqueness" UNIQUE ("uuid", "address")
)${extratableoptions};

CREATE INDEX "${tableprefix}address_index" ON "${tableprefix}addresses" ("address");

-- Exclusive outer join to return the most recent name of each player

CREATE VIEW "${tableprefix}latest_names" AS
  SELECT "names1"."uuid" "uuid", "names1"."name" "name", "names1"."updated" "updated"
  FROM "${tableprefix}names" "names1"
  LEFT JOIN "${tableprefix}names" "names2"
  ON "names1"."uuid" = "names2"."uuid"
  AND "names1"."updated" < "names2"."updated"
  WHERE "names2"."uuid" IS NULL;

-- Exclusive outer join to return the most recent address of each player

CREATE VIEW "${tableprefix}latest_addresses" AS
  SELECT "addrs1"."uuid" "uuid", "addrs1"."address" "address", "addrs1"."updated" "updated"
  FROM "${tableprefix}addresses" "addrs1"
  LEFT JOIN "${tableprefix}addresses" "addrs2"
  ON "addrs1"."uuid" = "addrs2"."uuid"
  AND "addrs1"."updated" < "addrs2"."updated"
  WHERE "addrs2"."address" IS NULL;
