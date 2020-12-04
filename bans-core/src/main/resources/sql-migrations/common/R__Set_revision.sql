
-- This table is special. See the PunishmentDatabase javadoc for more info

CREATE TABLE IF NOT EXISTS `libertybans_revision` (
`constant` ENUM('Constant') NOT NULL UNIQUE, 
`major` INT NOT NULL, 
`minor` INT NOT NULL);

INSERT INTO `libertybans_revision` (`constant`, `major`, `minor`) 
VALUES ('Constant', ${dbrevision.major}, ${dbrevision.minor})
ON DUPLICATE KEY UPDATE `major` = ${dbrevision.major}, `minor` = ${dbrevision.minor};
