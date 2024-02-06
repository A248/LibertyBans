CREATE TABLE "${tableprefix}undone" (
                                             "id" BIGINT NOT NULL,
                                             "operator" ${uuidtype} NOT NULL,
                                             "reason" CHARACTER VARYING(256) NOT NULL,
                                             "timestamp" BIGINT NOT NULL,
                                             CONSTRAINT "${tableprefix}undo_id_uniqueness" UNIQUE ("id"),
                                             CONSTRAINT "${tableprefix}undo_id_validity" FOREIGN KEY ("id") REFERENCES "${tableprefix}punishments" ("id") ON DELETE CASCADE
)${extratableoptions};

CREATE INDEX "${tableprefix}punishment_undo_index" ON "${tableprefix}undone" ("operator");

