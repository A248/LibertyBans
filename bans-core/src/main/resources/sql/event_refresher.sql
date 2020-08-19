DELIMITER //

CREATE EVENT `libertybans_refresher`
	ON SCHEDULE EVERY 3 HOUR
	DO
	BEGIN
	  START TRANSACTION;
	  DECLARE currentTime INT UNSIGNED DEFAULT 0;
	  SET currentTime = UNIX_TIMESTAMP();
	  DELETE `thetype` FROM `libertybans_bans` `thetype` INNER JOIN `libertybans_punishments` `puns` ON `puns`.`id` = `thetype`.`id` WHERE (`puns`.`end` != 0 AND `puns`.`end` < currentTime);
	  DELETE `thetype` FROM `libertybans_mutes` `thetype` INNER JOIN `libertybans_punishments` `puns` ON `puns`.`id` = `thetype`.`id` WHERE (`puns`.`end` != 0 AND `puns`.`end` < currentTime);
	  DELETE `thetype` FROM `libertybans_warns` `thetype` INNER JOIN `libertybans_punishments` `puns` ON `puns`.`id` = `thetype`.`id` WHERE (`puns`.`end` != 0 AND `puns`.`end` < currentTime);
	  COMMIT;
  END;
//

DELIMITER ;
