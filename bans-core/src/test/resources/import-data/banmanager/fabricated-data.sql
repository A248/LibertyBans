
INSERT INTO "bm_ip_bans" ("id", "ip", "reason", "actor_id", "created", "updated", "expires", "silent") VALUES
(1, X'7f000001', 'now your IP is banned too', X'09130bc4b0cd4e268e13a1c1d682a2b0', 1642364184, 1642364184, 0, 0);

INSERT INTO "bm_ip_mute_records" ("id", "ip", "reason", "expired", "actor_id", "pastActor_id", "pastCreated", "created", "createdReason", "soft", "silent") VALUES
(1, X'7f000001', 'and your IP is muted as well', 0, X'09130bc4b0cd4e268e13a1c1d682a2b0', X'09130bc4b0cd4e268e13a1c1d682a2b0', 1642364205, 1642364339, '', 0, 0),
(2, X'7f000001', 'again your IP is muted', 0, X'09130bc4b0cd4e268e13a1c1d682a2b0', X'09130bc4b0cd4e268e13a1c1d682a2b0', 1642364362, 1642364557, '', 0, 0);

INSERT INTO "bm_players" ("id", "name", "ip", "lastSeen") VALUES
(X'09130bc4b0cd4e268e13a1c1d682a2b0', 'Console', X'7f000001', 1642364025),
(X'ed5f12cd600745d9a4b9940524ddaecf', 'A248', X'7f000001', 1642364163);

INSERT INTO "bm_player_bans" ("id", "player_id", "reason", "actor_id", "created", "updated", "expires", "silent") VALUES
(1, X'ed5f12cd600745d9a4b9940524ddaecf', 'no reason at all', X'09130bc4b0cd4e268e13a1c1d682a2b0', 1642364163, 1642364163, 0, 0);

INSERT INTO "bm_player_mute_records" ("id", "player_id", "reason", "expired", "actor_id", "pastActor_id", "pastCreated", "created", "createdReason", "soft", "silent") VALUES
(1, X'ed5f12cd600745d9a4b9940524ddaecf', 'temporarily muted', 1644957032, X'09130bc4b0cd4e268e13a1c1d682a2b0', X'09130bc4b0cd4e268e13a1c1d682a2b0', 1642365032, 1642365161, 'A248', 0, 0);

INSERT INTO "bm_player_report_states" ("id", "name") VALUES
(1, 'Open'),
(2, 'Assigned'),
(3, 'Resolved'),
(4, 'Closed');

INSERT INTO "bm_player_warnings" ("id", "player_id", "reason", "actor_id", "created", "expires", "read", "points") VALUES
(1, X'ed5f12cd600745d9a4b9940524ddaecf', 'would you care for a warn?', X'09130bc4b0cd4e268e13a1c1d682a2b0', 1642364319, 0, 0, 1);
