
CREATE TABLE "${tableprefix}scopes" (
  "id" INT NOT NULL,
  "type" SMALLINT NOT NULL,
  "value" CHARACTER VARYING(32) NOT NULL,
  CONSTRAINT "${tableprefix}scope_id_uniqueness" UNIQUE ("id"),
  CONSTRAINT "${tableprefix}scope_type_validity" CHECK ("type" IN (1, 2)),
  CONSTRAINT "${tableprefix}scope_data_uniqueness" UNIQUE ("type", "value")
)${extratableoptions};

-- Include the scope ID column in the punishments table
-- For backwards compatibility, the old textual scope column is kept
ALTER TABLE "${tableprefix}punishments" ADD COLUMN "scope_id" INT NULL;

-- Recreate every punishment view to include the scope-related columns

-- Bans

${alterviewstatement} "${tableprefix}simple_bans" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopestart}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopeend} AS "scope",
    "puns"."start", "puns"."end",
    (CASE
      WHEN "tracks"."namespace" IS NULL THEN NULL
      ELSE (("tracks"."namespace" || ':') || "tracks"."value")
    END) AS "track",
    (CASE
      WHEN "puns"."scope_id" IS NULL THEN ${zerosmallintliteral}
      ELSE "scopes"."type"
    END) AS "scope_type"
  FROM "${tableprefix}bans" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id"
  LEFT JOIN "${tableprefix}tracks" AS "tracks"
  ON "puns"."track" = "tracks"."id"
  LEFT JOIN "${tableprefix}scopes" AS "scopes"
  ON "puns"."scope_id" = "scopes"."id";

${alterviewstatement} "${tableprefix}applicable_bans" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_bans" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Mutes

${alterviewstatement} "${tableprefix}simple_mutes" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopestart}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopeend} AS "scope",
    "puns"."start", "puns"."end",
    (CASE
      WHEN "tracks"."namespace" IS NULL THEN NULL
      ELSE (("tracks"."namespace" || ':') || "tracks"."value")
    END) AS "track",
    (CASE
      WHEN "puns"."scope_id" IS NULL THEN ${zerosmallintliteral}
      ELSE "scopes"."type"
    END) AS "scope_type"
  FROM "${tableprefix}mutes" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id"
  LEFT JOIN "${tableprefix}tracks" AS "tracks"
  ON "puns"."track" = "tracks"."id"
  LEFT JOIN "${tableprefix}scopes" AS "scopes"
  ON "puns"."scope_id" = "scopes"."id";

${alterviewstatement} "${tableprefix}applicable_mutes" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_mutes" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Warns

${alterviewstatement} "${tableprefix}simple_warns" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopestart}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopeend} AS "scope",
    "puns"."start", "puns"."end",
    (CASE
      WHEN "tracks"."namespace" IS NULL THEN NULL
      ELSE (("tracks"."namespace" || ':') || "tracks"."value")
    END) AS "track",
    (CASE
      WHEN "puns"."scope_id" IS NULL THEN ${zerosmallintliteral}
      ELSE "scopes"."type"
    END) AS "scope_type"
  FROM "${tableprefix}warns" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id"
  LEFT JOIN "${tableprefix}tracks" AS "tracks"
  ON "puns"."track" = "tracks"."id"
  LEFT JOIN "${tableprefix}scopes" AS "scopes"
  ON "puns"."scope_id" = "scopes"."id";

${alterviewstatement} "${tableprefix}applicable_warns" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_warns" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Other helpers

${alterviewstatement} "${tableprefix}simple_history" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopestart}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopeend} AS "scope",
    "puns"."start", "puns"."end",
    (CASE
      WHEN "tracks"."namespace" IS NULL THEN NULL
      ELSE (("tracks"."namespace" || ':') || "tracks"."value")
    END) AS "track",
    (CASE
      WHEN "puns"."scope_id" IS NULL THEN ${zerosmallintliteral}
      ELSE "scopes"."type"
    END) AS "scope_type"
  FROM "${tableprefix}history" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id"
  LEFT JOIN "${tableprefix}tracks" AS "tracks"
  ON "puns"."track" = "tracks"."id"
  LEFT JOIN "${tableprefix}scopes" AS "scopes"
  ON "puns"."scope_id" = "scopes"."id";

${alterviewstatement} "${tableprefix}simple_active" AS
  SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "track", "scope_type"
  FROM "${tableprefix}simple_bans"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "track", "scope_type"
    FROM "${tableprefix}simple_mutes"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "track", "scope_type"
    FROM "${tableprefix}simple_warns";

${alterviewstatement} "${tableprefix}applicable_active" AS
  SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address", "track", "scope_type"
  FROM "${tableprefix}applicable_bans"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address", "track", "scope_type"
    FROM "${tableprefix}applicable_mutes"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address", "track", "scope_type"
    FROM "${tableprefix}applicable_warns";

${alterviewstatement} "${tableprefix}applicable_history" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_history" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));
