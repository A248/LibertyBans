
CREATE VIEW "${tableprefix}applicable_history" AS
  SELECT "puns"."id", "puns"."type", "puns"."victim_type", "puns"."victim_uuid", "puns"."victim_address", "puns"."operator",
    "puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address"
  FROM "${tableprefix}simple_history" AS "puns"
  INNER JOIN "${tableprefix}addresses" AS "addrs"
  ON ("puns"."victim_type" = 0 AND "puns"."victim_uuid" = "addrs"."uuid"
    OR "puns"."victim_type" = 1 AND "puns"."victim_address" = "addrs"."address"
    OR "puns"."victim_type" = 2 AND ("puns"."victim_uuid" = "addrs"."uuid" OR "puns"."victim_address" = "addrs"."address"));
