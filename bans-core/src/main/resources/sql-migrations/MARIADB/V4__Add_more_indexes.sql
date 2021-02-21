
CREATE INDEX username_index ON `libertybans_names` (`name`);

CREATE INDEX operator_index ON `libertybans_punishments` (`operator` (8));
CREATE INDEX endtime_index ON `libertybans_punishments` (`end`);

CREATE INDEX victim_index ON `libertybans_warns` (`victim_type`, `victim` (8));
CREATE INDEX victim_index ON `libertybans_history` (`victim_type`, `victim` (8));
