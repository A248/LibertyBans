
-- Ban

CREATE VIEW "${zeroeighttableprefix}simple_bans" AS 
SELECT "puns"."id", "puns"."type", "thetype"."victim", "thetype"."victim_type", 
"puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end" 
FROM "${zeroeighttableprefix}bans" "thetype" INNER JOIN "${zeroeighttableprefix}punishments" "puns" 
ON "puns"."id" = "thetype"."id";

CREATE VIEW "${zeroeighttableprefix}applicable_bans" AS 
SELECT "puns"."id", "puns"."type", "puns"."victim", "puns"."victim_type", "puns"."operator", 
"puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address" 
FROM "${zeroeighttableprefix}simple_bans" "puns" INNER JOIN "${zeroeighttableprefix}addresses" "addrs" 
ON ("puns"."victim_type" = 'PLAYER' AND "puns"."victim" = "addrs"."uuid" 
OR "puns"."victim_type" = 'ADDRESS' AND "puns"."victim" = "addrs"."address");

-- Mute

CREATE VIEW "${zeroeighttableprefix}simple_mutes" AS 
SELECT "puns"."id", "puns"."type", "thetype"."victim", "thetype"."victim_type", 
"puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end" 
FROM "${zeroeighttableprefix}mutes" "thetype" INNER JOIN "${zeroeighttableprefix}punishments" "puns" 
ON "puns"."id" = "thetype"."id";

CREATE VIEW "${zeroeighttableprefix}applicable_mutes" AS 
SELECT "puns"."id", "puns"."type", "puns"."victim", "puns"."victim_type", "puns"."operator", 
"puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address" 
FROM "${zeroeighttableprefix}simple_mutes" "puns" INNER JOIN "${zeroeighttableprefix}addresses" "addrs" 
ON ("puns"."victim_type" = 'PLAYER' AND "puns"."victim" = "addrs"."uuid" 
OR "puns"."victim_type" = 'ADDRESS' AND "puns"."victim" = "addrs"."address");

-- Warn

CREATE VIEW "${zeroeighttableprefix}simple_warns" AS 
SELECT "puns"."id", "puns"."type", "thetype"."victim", "thetype"."victim_type", 
"puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end" 
FROM "${zeroeighttableprefix}warns" "thetype" INNER JOIN "${zeroeighttableprefix}punishments" "puns" 
ON "puns"."id" = "thetype"."id";

CREATE VIEW "${zeroeighttableprefix}applicable_warns" AS 
SELECT "puns"."id", "puns"."type", "puns"."victim", "puns"."victim_type", "puns"."operator", 
"puns"."reason", "puns"."scope", "puns"."start", "puns"."end", "addrs"."uuid", "addrs"."address" 
FROM "${zeroeighttableprefix}simple_warns" "puns" INNER JOIN "${zeroeighttableprefix}addresses" "addrs" 
ON ("puns"."victim_type" = 'PLAYER' AND "puns"."victim" = "addrs"."uuid" 
OR "puns"."victim_type" = 'ADDRESS' AND "puns"."victim" = "addrs"."address");

-- Other helpers

CREATE VIEW "${zeroeighttableprefix}simple_history" AS 
SELECT "puns"."id", "puns"."type", "thetype"."victim", "thetype"."victim_type", 
"puns"."operator", "puns"."reason", "puns"."scope", "puns"."start", "puns"."end" 
FROM "${zeroeighttableprefix}history" "thetype" INNER JOIN "${zeroeighttableprefix}punishments" "puns" 
ON "puns"."id" = "thetype"."id";

CREATE VIEW "${zeroeighttableprefix}simple_active" AS 
SELECT * FROM "${zeroeighttableprefix}simple_bans" UNION ALL 
SELECT * FROM "${zeroeighttableprefix}simple_mutes" UNION ALL 
SELECT * FROM "${zeroeighttableprefix}simple_warns";

-- Strict account links

CREATE VIEW "${zeroeighttableprefix}strict_links" AS
SELECT "addrs1"."uuid" "uuid1", "addrs2"."uuid" "uuid2"
FROM "${zeroeighttableprefix}addresses" "addrs1" INNER JOIN "${zeroeighttableprefix}addresses" "addrs2" 
ON "addrs1"."address" = "addrs2"."address";
