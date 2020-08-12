-- The following 2 queries are executed for each punishment type with the exclusion of KICK
-- They are both designed to simplify querying active punishments
-- <lowerNamePlural> is replaced with PunishmentType#getLowercaseNamePlural

CREATE VIEW `libertybans_simple_<lowerNamePlural>` AS 
SELECT `puns`.`id`, `puns`.`type`, `thetype`.`victim`, `thetype`.`victim_type`, 
`puns`.`operator`, `puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end` 
FROM `libertybans_<lowerNamePlural>` `thetype` INNER JOIN `libertybans_punishments` `puns` 
ON `puns`.`id` = `thetype`.`id`;

CREATE VIEW `libertybans_applicable_<lowerNamePlural>` AS 
SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, 
`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` 
FROM `libertybans_simple_<lowerNamePlural>` `puns` INNER JOIN `libertybans_addresses` `addrs` 
ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` 
OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`);

-- This query is executed only once

CREATE VIEW `libertybans_simple_active` AS 
SELECT * FROM `libertybans_simple_bans` UNION ALL 
SELECT * FROM `libertybans_simple_mutes` UNION ALL 
SELECT * FROM `libertybans_simple_warns`;
