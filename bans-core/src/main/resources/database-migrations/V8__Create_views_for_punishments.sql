
-- Bans

CREATE VIEW "${tableprefix}simple_bans" AS 
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end"
  FROM "${tableprefix}bans" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id";

CREATE VIEW "${tableprefix}applicable_bans" AS 
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address", "puns"."operator",
    "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address"
  FROM "${tableprefix}simple_bans" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Mutes

CREATE VIEW "${tableprefix}simple_mutes" AS 
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end"
  FROM "${tableprefix}mutes" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id";

CREATE VIEW "${tableprefix}applicable_mutes" AS 
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address", "puns"."operator",
    "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address"
  FROM "${tableprefix}simple_mutes" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Warns

CREATE VIEW "${tableprefix}simple_warns" AS 
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end"
  FROM "${tableprefix}warns" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id";

CREATE VIEW "${tableprefix}applicable_warns" AS 
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address", "puns"."operator",
    "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address"
  FROM "${tableprefix}simple_warns" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));

-- Other helpers

CREATE VIEW "${tableprefix}simple_history" AS
  SELECT "puns"."id", "puns"."type",
    "victims"."type" AS "victim_type", "victims"."uuid" AS "victim_uuid", "victims"."address" AS "victim_address",
    "puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end"
  FROM "${tableprefix}history" AS "thetype"
  INNER JOIN "${tableprefix}punishments" AS "puns"
  ON "thetype"."id" = "puns"."id"
  INNER JOIN "${tableprefix}victims" AS "victims"
  ON "thetype"."victim" = "victims"."id";

CREATE VIEW "${tableprefix}simple_active" AS
  SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end"
  FROM "${tableprefix}simple_bans"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end"
    FROM "${tableprefix}simple_mutes"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end"
    FROM "${tableprefix}simple_warns";

CREATE VIEW "${tableprefix}applicable_active" AS
  SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address"
  FROM "${tableprefix}applicable_bans"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address"
    FROM "${tableprefix}applicable_mutes"
  UNION ALL
    SELECT "id", "type", "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope", "start", "end", "uuid", "address"
    FROM "${tableprefix}applicable_warns";

-- Strict account links

CREATE VIEW "${tableprefix}strict_links" AS
  SELECT "addrs1"."uuid" AS "uuid1", "addrs2"."uuid" AS "uuid2"
  FROM "${tableprefix}addresses" AS "addrs1"
  INNER JOIN "${tableprefix}addresses" AS "addrs2"
  ON "addrs1"."address" = "addrs2"."address";
