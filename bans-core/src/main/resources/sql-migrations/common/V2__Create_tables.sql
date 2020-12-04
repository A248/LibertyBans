
-- Tables

CREATE TABLE `libertybans_names` (
`uuid` BINARY(16) NOT NULL, 
`name` VARCHAR(16) NOT NULL, 
`updated` BIGINT NOT NULL, 
PRIMARY KEY (`uuid`, `name`));

/*
The reason this is not the same table as the last is that some servers
may need to periodically clear the addresses table per GDPR regulations.
Using a single table with a nullable address would be unwise, since
unique constraints don't work nicely with null values.
*/

CREATE TABLE `libertybans_addresses` (
`uuid` BINARY(16) NOT NULL, 
`address` VARBINARY(16) NOT NULL, 
`updated` BIGINT NOT NULL, 
PRIMARY KEY (`uuid`, `address`));

-- Primary punishments table from which others are derived

CREATE TABLE `libertybans_punishments` (
`id` INT AUTO_INCREMENT PRIMARY KEY, 
`type` ENUM ('BAN', 'MUTE', 'WARN', 'KICK') NOT NULL, 
`operator` BINARY(16) NOT NULL, 
`reason` VARCHAR(256) NOT NULL, 
`scope` VARCHAR(32) NOT NULL, 
`start` BIGINT NOT NULL, 
`end` BIGINT NOT NULL);

-- Individual punishment tables
-- These are separate so that they may have different constraints

CREATE TABLE `libertybans_bans` (
`id` INT PRIMARY KEY, 
`victim` VARBINARY(16) NOT NULL, 
`victim_type` ENUM('PLAYER', 'ADDRESS') NOT NULL, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE, 
UNIQUE (`victim`, `victim_type`));

CREATE TABLE `libertybans_mutes` (
`id` INT PRIMARY KEY, 
`victim` VARBINARY(16) NOT NULL, 
`victim_type` ENUM('PLAYER', 'ADDRESS') NOT NULL, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE, 
UNIQUE (`victim`, `victim_type`));

CREATE TABLE `libertybans_warns` (
`id` INT PRIMARY KEY, 
`victim` VARBINARY(16) NOT NULL, 
`victim_type` ENUM('PLAYER', 'ADDRESS') NOT NULL, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE);

CREATE TABLE `libertybans_history` (
`id` INT PRIMARY KEY, 
`victim` VARBINARY(16) NOT NULL, 
`victim_type` ENUM('PLAYER', 'ADDRESS') NOT NULL, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE);
