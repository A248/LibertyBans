DELIMITER //

CREATE PROCEDURE `libertybans_kicktallier` (
IN victim_var VARBINARY(16),
IN victim_type_var INT,
IN operator_var BINARY(16),
IN reason_var VARCHAR(256),
IN scope_var VARCHAR(32),
IN start_var BIGINT,
IN end_var BIGINT
)
BEGIN
	
	START TRANSACTION;

	INSERT INTO `libertybans_punishments` (`type`, `operator`, `reason`, `scope`, `start`, `end`) VALUES ('KICK', operator_var, reason_var, scope_var, start_var, end_var);

	INSERT INTO `libertybans_history` (`id`, `victim`, `victim_type`) VALUES (LAST_INSERT_ID(), victim_var, victim_type_var);

	COMMIT;
	SELECT LAST_INSERT_ID() AS `id`;

END //

DELIMITER ;