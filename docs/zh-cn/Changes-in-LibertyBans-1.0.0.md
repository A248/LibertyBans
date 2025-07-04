
This is a formal change-log of changes made in LibertyBans 1.0.0. It notes all manners of changes, including breaking changes; technically-detailed, little-mentioned changes which are invisible to the user; sweeping feature additions, and in-depth descriptions thereof; design and architectural decisions, and various minor matters.

If you are interested in a guide for upgrading to LibertyBans 0.8.x to 1.0.0, which focuses on breaking changes and their impact to you, see [this page](Upgrading-to-LibertyBans-1.0.0-from-0.8.x)

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
* Added the composite victim type, a new kind of victim which is both a UUID and address.
  * Composite victims are a better choice when you want to IP-ban users by default, but do not want the technical intricacies associated with banning an IP address rather than a user. In other words, you want user bans to be enforced as IP-bans but be able to punish, unpunish, and list punishments as if you were doing so for a user rather than an IP address.
  * See the wiki page on composite punishments for more details.
* The /accounthistory command accepts the `-both` argument to show the accounts matching the specified user's UUID or current IP address.
* HikariCP is now relocated in release builds.
* New variables are added to punishment messages:
  * %TYPE_VERB% - verb-based rendition of the punishment type
  * %TIME_PASSED_SIMPLE% - like TIME_PASSED but rounds to the largest time unit
  * %TIME_REMAINING_SIMPLE% - like TIME_REMAINING but rounds to the largest time unit
  * %HAS_EXPIRED% - whether the punishment has expired on account of the passage of time
* Added the %PREVIOUSPAGE% variable

### API Changes

* The API now uses `long` (64-bit integers) for punishment IDs. To migrate usage, cast to long or refactor to use long IDs.
  * `getID` is deprecated but kept for backwards compatibility. Please migrate to `getIdentifier` when possible.
* `EnforcementOptions` has been added to the API and replaces some methods in DraftPunishment, Punishment, and RevocationOrder. If you were using any of the 'withoutEnforcement' or 'withoutUnenforcement' methods, you will need to change your code. Usage can be replaced as follows:
  * For DraftPunishment: `draftPunishment.enactPunishmentWithoutEnforcement()` -> `draftPunishment.enactPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build())`
  * For Punishment:
    * `punishment.undoPunishmentWithoutUnenforcement()` -> `punishment.undoPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build)`
    * `punishment.unenforcePunishment()` -> `punishment.unenforcePunishment
  * For RevocationOrder:
    * `revocationOrder.undoPunishmentWithoutUnenforcement()` -> `revocationOrder.undoPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build())`
    * `revocationOrder.undoAndGetPunishmentWithoutUnenforcement()` -> `revocationOrder.undoAndGetPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build())`
* The package `space.arim.libertybans.api.revoke` has been merged into `space.arim.libertybans.api.punish`. Imports should be updated accordingly.
* The API for selecting punishments has been expanded in capability. Keyset pagination is now possible. As part of this change, `SelectionOrderBuilder` and `SelectionOrder` have breaking changes:
  * `SelectionOrderBuilder#maximumToRetrieve` has been removed, in favor of `#limitToRetrieve` which is consistent with SQL's LIMIT.
* Non-breaking improvements to `PunishmentRevoker`:
  * The requirement that the punishment type be singular (PunishmentType.isSingular()) is lifted for `PunishmentRevoker#revokeByTypeAndVictim`. However, if a non-singular punishment type is used, it is unspecified behavior as to which punishment will be revoked.
  * Added the method `PunishmentRevoker#revokeByTypeAndPossibleVictims`.
* Other breaking changes are only relevant if you were implementing the API yourself, which is unlikely:
  * Getter methods on `SelectionOrder` reflect the new and expanded API.
  * Getter methods on `RevocationOrder` reflect the expanded API.
* The Punishment.PERMANENT_END_DATE constant is added to the API.
* It is now possible to dispatch "silent" punishments using the API.
* Added `PunishmentFormatter#formatPunishmentTypeVerb`
