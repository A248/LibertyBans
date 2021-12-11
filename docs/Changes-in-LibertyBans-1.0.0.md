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
