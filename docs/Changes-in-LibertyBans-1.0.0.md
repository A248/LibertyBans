This is a formal change-log of changes made in LibertyBans 1.0.0. It notes all manners of changes, including breaking changes; technically-detailed, little-mentioned changes which are invisible to the user; sweeping feature additions, and in-depth descriptions thereof; design and architectural decisions, and various minor matters.

If you are interested in a guide for upgrading to LibertyBans 0.8.x to 1.0.0, which focuses on breaking changes and their impact to you,

## Changes

* Permissions have been refactored and made into a logically intuitive pattern.
* Permission messages have been re-organized using `PunishmentPermissionSection`, which is used consistently for each punishment type with regard to punishment additions and removals.
* Compatibility is removed with a bugged duration permissions format from LibertyBans 0.7.6
* MariaDB 10.2 and MySQL 5.7 are no longer supported.
* The `playerOrAddress` option in the messages.yml is now `player-or-address`, `permission.command` is now `permission.uuid` in several places, and `banList` and `muteList` become `ban-list` and `mute-list`
* The schema history for 1.0.0 is made anew, so older versions like 0.8.2 will fail to use the new database. To ensure seamless transition of existing data, a Java-based Flyway migration will exist in LibertyBans 1.0.0, which will detect existing 0.8.2 tables and transfer their data to the new tables. The Java-based migration will be removed in LibertyBans 1.1.0; Java-based migrations do not use checksums in the same manner as normal Flyway migrations.
* All SQL queries are rewritten to use JOOQ. The build runs JOOQ's code generation. This change enables PostgreSQL compatibility, as queries previously used back-ticks.
* Integration tests now start instances of PostgreSQL in addition to MariaDB.
* Flyway is updated to 8.x
* The event scheduler feature for MySQL/MariaDB has been removed. Its marginal benefit does not justify the maintenance cost.
* Added accepted value 'MYSQL' for `rdms-vendor` and require MySQL to be distinguished from MariaDB.
* LibertyBans now handles transaction serialization failure and will retry transactions which failed due to contention:
  * Serialization failure describes the situation where multiple database transactions operate on the same data, and somehow conflict with one another. Serialization failures are propagated to the application.
  * Whether software handles serialization failure has nothing to do with whether it maintains the integrity of its data: it is not possible to corrupt data merely due to a serialization failure.
  * This means, in rare cases where running a command would result in a serialization failure, LibertyBans 1.0.0 will handle the situation gracefully, whereas LibertyBans 0.8.x will fail with an exception. Symptoms in 0.8.x would include stacktraces in the server console. However, in practice, no one has reported any instance of serialization failure in LibertyBans.
  * The handling of transaction serialization failure will not be back-ported to LibertyBans 0.8.x. Much software does not handle transaction serialization failure (including LibertyBans 0.8.x) therefore this is not considered a bug of sufficient importance.
  * Further technical reading: https://stackoverflow.com/questions/7705273/what-are-the-conditions-for-encountering-a-serialization-failure
* Relevant client encoding variables are set per each database upon establishing connection. Also, the charset utf8mb4 and collation utf8mb4_bin are now set on created tables for MariaDB and MySQL.
* Punishment.PERMANENT_END_DATE constant is added to the API
* The API now uses `long` (64-bit integers) for punishment IDs.
