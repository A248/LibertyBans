CREATE EVENT IF NOT EXISTS `libertybans_refresher`
	ON SCHEDULE EVERY 3 HOUR
	DO
		CALL `libertybans_refresh`();
