CREATE TABLE "${tableprefix}undone" (
                                             "id" BIGINT NOT NULL,
                                             "operator" ${uuidtype} NOT NULL,
                                             "reason" CHARACTER VARYING(256) NOT NULL,
                                             "time" BIGINT NOT NULL,
                                             CONSTRAINT "${tableprefix}undo_id_uniqueness" UNIQUE ("id"),
                                             CONSTRAINT "${tableprefix}undo_id_validity" FOREIGN KEY ("id") REFERENCES "${tableprefix}punishments" ("id") ON DELETE CASCADE
)${extratableoptions};

CREATE INDEX "${tableprefix}punishment_undone_index" ON "${tableprefix}undone" ("operator");

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
           END) AS "scope_type",
        "undone"."operator" AS "undo_operator",
        "undone"."reason" AS "undo_reason",
        "undone"."time" AS "undo_time"
FROM "${tableprefix}history" AS "thetype"
         INNER JOIN "${tableprefix}punishments" AS "puns"
                    ON "thetype"."id" = "puns"."id"
         INNER JOIN "${tableprefix}victims" AS "victims"
                    ON "thetype"."victim" = "victims"."id"
         LEFT JOIN "${tableprefix}tracks" AS "tracks"
                   ON "puns"."track" = "tracks"."id"
         LEFT JOIN "${tableprefix}scopes" AS "scopes"
                   ON "puns"."scope_id" = "scopes"."id"
         LEFT JOIN "${tableprefix}undone" AS "undone"
                   ON "puns"."id" = "undone"."id"
;

