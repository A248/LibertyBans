
-- Views

/*

Create views

The following 2 queries are executed for each punishment type with the exclusion of KICK
They are both designed to simplify querying active punishments
Replace <lowerNamePlural> is replaced with the relevant punishment type
*/

/*
CREATE OR REPLACE VIEW `libertybans_simple_<lowerNamePlural>` AS 
SELECT `puns`.`id`, `puns`.`type`, `thetype`.`victim`, `thetype`.`victim_type`, 
`puns`.`operator`, `puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end` 
FROM `libertybans_<lowerNamePlural>` `thetype` INNER JOIN `libertybans_punishments` `puns` 
ON `puns`.`id` = `thetype`.`id`;

CREATE OR REPLACE VIEW `libertybans_applicable_<lowerNamePlural>` AS 
SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, 
`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` 
FROM `libertybans_simple_<lowerNamePlural>` `puns` INNER JOIN `libertybans_addresses` `addrs` 
ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` 
OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`);
*/

-- Ban

CREATE VIEW `libertybans_simple_bans` AS 
SELECT `puns`.`id`, `puns`.`type`, `thetype`.`victim`, `thetype`.`victim_type`, 
`puns`.`operator`, `puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end` 
FROM `libertybans_bans` `thetype` INNER JOIN `libertybans_punishments` `puns` 
ON `puns`.`id` = `thetype`.`id`;

CREATE VIEW `libertybans_applicable_bans` AS 
SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, 
`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` 
FROM `libertybans_simple_bans` `puns` INNER JOIN `libertybans_addresses` `addrs` 
ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` 
OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`);

-- Mute

CREATE VIEW `libertybans_simple_mutes` AS 
SELECT `puns`.`id`, `puns`.`type`, `thetype`.`victim`, `thetype`.`victim_type`, 
`puns`.`operator`, `puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end` 
FROM `libertybans_mutes` `thetype` INNER JOIN `libertybans_punishments` `puns` 
ON `puns`.`id` = `thetype`.`id`;

CREATE VIEW `libertybans_applicable_mutes` AS 
SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, 
`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` 
FROM `libertybans_simple_mutes` `puns` INNER JOIN `libertybans_addresses` `addrs` 
ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` 
OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`);

-- Warn

CREATE VIEW `libertybans_simple_warns` AS 
SELECT `puns`.`id`, `puns`.`type`, `thetype`.`victim`, `thetype`.`victim_type`, 
`puns`.`operator`, `puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end` 
FROM `libertybans_warns` `thetype` INNER JOIN `libertybans_punishments` `puns` 
ON `puns`.`id` = `thetype`.`id`;

CREATE VIEW `libertybans_applicable_warns` AS 
SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, 
`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` 
FROM `libertybans_simple_warns` `puns` INNER JOIN `libertybans_addresses` `addrs` 
ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` 
OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`);

-- Other helpers

CREATE VIEW `libertybans_simple_history` AS 
SELECT `puns`.`id`, `puns`.`type`, `thetype`.`victim`, `thetype`.`victim_type`, 
`puns`.`operator`, `puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end` 
FROM `libertybans_history` `thetype` INNER JOIN `libertybans_punishments` `puns` 
ON `puns`.`id` = `thetype`.`id`;

CREATE VIEW `libertybans_simple_active` AS 
SELECT * FROM `libertybans_simple_bans` UNION ALL 
SELECT * FROM `libertybans_simple_mutes` UNION ALL 
SELECT * FROM `libertybans_simple_warns`;

-- Strict account links

CREATE VIEW `libertybans_strict_links` AS
SELECT `addrs1`.`uuid` `uuid1`, `addrs2`.`uuid` `uuid2`
FROM `libertybans_addresses` `addrs1` INNER JOIN `libertybans_addresses` `addrs2` 
ON `addrs1`.`address` = `addrs2`.`address`;
