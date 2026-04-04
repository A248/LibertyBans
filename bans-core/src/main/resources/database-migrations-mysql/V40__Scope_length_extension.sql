
-- Update column length

ALTER TABLE "${tableprefix}scopes" MODIFY "value" CHARACTER VARYING(255) NOT NULL, ALGORITHM = INSTANT;

-- Recreate views with the updated-length scope column

-- Bans

ALTER VIEW "${tableprefix}simple_bans" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopepost343start}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopepost343end} AS "scope",
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

ALTER VIEW "${tableprefix}applicable_bans" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_bans" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Mutes

ALTER VIEW "${tableprefix}simple_mutes" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopepost343start}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopepost343end} AS "scope",
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

ALTER VIEW "${tableprefix}applicable_mutes" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_mutes" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Warns

ALTER VIEW "${tableprefix}simple_warns" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopepost343start}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopepost343end} AS "scope",
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

ALTER VIEW "${tableprefix}applicable_warns" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_warns" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Other helpers

ALTER VIEW "${tableprefix}simple_history" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason",
    ${migratescopepost343start}(CASE
      WHEN "puns"."scope_id" IS NULL THEN ''
      ELSE "scopes"."value"
    END)${migratescopepost343end} AS "scope",
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

ALTER VIEW "${tableprefix}simple_active" AS
  SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "track", "scope_type"
  FROM "${tableprefix}simple_bans"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "track", "scope_type"
    FROM "${tableprefix}simple_mutes"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "track", "scope_type"
    FROM "${tableprefix}simple_warns";

ALTER VIEW "${tableprefix}applicable_active" AS
  SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address", "track", "scope_type"
  FROM "${tableprefix}applicable_bans"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address", "track", "scope_type"
    FROM "${tableprefix}applicable_mutes"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address", "track", "scope_type"
    FROM "${tableprefix}applicable_warns";

ALTER VIEW "${tableprefix}applicable_history" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address",
    "puns"."track", "puns"."scope_type"
  FROM "${tableprefix}simple_history" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));
