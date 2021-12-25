
-- Exclusive outer join to return the most recent name of each player

CREATE VIEW "${zeroeighttableprefix}latest_names" AS
  SELECT "names1"."uuid" "uuid", "names1"."name" "name", "names1"."updated" "updated"
  FROM "${zeroeighttableprefix}names" "names1"
  LEFT JOIN "${zeroeighttableprefix}names" "names2"
  ON "names1"."uuid" = "names2"."uuid"
  AND "names1"."updated" < "names2"."updated"
  WHERE "names2"."uuid" IS NULL;

-- Exclusive outer join to return the most recent address of each player

CREATE VIEW "${zeroeighttableprefix}latest_addresses" AS
  SELECT "addrs1"."uuid" "uuid", "addrs1"."address" "address", "addrs1"."updated" "updated"
  FROM "${zeroeighttableprefix}addresses" "addrs1"
  LEFT JOIN "${zeroeighttableprefix}addresses" "addrs2"
  ON "addrs1"."uuid" = "addrs2"."uuid"
  AND "addrs1"."updated" < "addrs2"."updated"
  WHERE "addrs2"."address" IS NULL;
