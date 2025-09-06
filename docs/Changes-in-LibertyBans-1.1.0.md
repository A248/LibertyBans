
## Changes

* Require Java 17.
* Sponge API 12 and 13 are now usable with LibertyBans, while API 8 and 9 are still supported.
* Support Folia, a Paper-derived server software for multithreaded regioning.
* Using MariaDB with LibertyBans now requires MariaDB 10.6 at minimum.
  * MariaDB 10.3, 10.4., 10.5 is given "retrograde" support, meaning that it is heavily discouraged to run these versions and they require query rewriting.
* Automatic detection of Geyser/Floodgate and determination of the Bedrock name prefix.
  * The `GEYSER` server-type option has been removed. This server type was inflexible; it failed to distinguish between online and offline Geyser-utilizing servers. It is obsolete due to automatic Floodgate detection.
  * The name prefix is queried from the Floodgate API at startup.
* Before this release, `STRICT` address-strictness enforced user punishments too stringently - as if they were IP-based punishments - which was contrary to the documentation.
  * Because it is impossible to retroactively change the behavior of `STRICT` without breaking existing setups, a new address-strictness setting `STERN` is added. `STERN` behaves the way `STRICT` was previously documented to.
  * The `STRICT` documentation has updated to reflect its full behavior.
  * See the wiki page for more information.
* In the rare case that multiple mutes are issued very quickly, the `warn-actions` addon will make sure warns do not "overlap" one another. There were no reports of this happening, but it was a theoretical possibility. The solution works by leveraging the newly-added `seekBefore` API.
* Snapshot versions are now differentiated according to the build timestamp.
* The database-related thread pool is now fully shut down and its termination awaited before the connection pool is closed. This prevents a harmless exception which occurred when shutdown coincided with the periodic synchronization task.

### Features

* Added the exemption feature. To support exemption flexibly, different exemption backends are offered:
  * The `exemption-luckperms` addon depends on LuckPerms and uses LuckPerms' group weights to define the exemption hierarchy.
  * The `exemption-vault` addon depends on Vault and works only on Bukkit. It uses tiered permissions to define exemption levels.
  * If their dependencies are not met, or if they are installed on the wrong platform, these addons will not load.
  * See the wiki for documentation on this feature.
* Added layouts. Leveling punishments, tracks, ladders -- the feature has many names.
  * The `layouts` addon should be installed to enable this feature. Configuration is described on the wiki.
* Added server scopes.
  * Scopes and scope categories can be written in commands with, for example, `-scope=<scope>` and `-category=<category>`. Alternatively, the default scope can be configured if the staff member does not write one.
  * A new `scope.yml` configuration file will now be generated for each server instance. This file controls that instance's configuration, and can be used to set the instance's scope name as well as any scope categories it belongs to. It has options to detect the backend instance's server name by querying the proxy.
* Added `command-expunge` and `command-extend` addons. These addons let you either purge punishments entirely, or extend their time before expiration, depending on your fancy.
* Hide or censor IP addresses for players without permission.

### API Changes

* Add target username in `PostPunishEvent` / `PostPardonEvent`
* Expand selection capabilities and complete APIs:
  * `SelectionOrder#countNumberOfPunishments` yields the pure number of punishments, which is more efficient than retrieving the punishments themselves from the database.
  * `SelectionOrderBuilder#seekBefore` allows retrieving punishments before a specified time / ID. It is the counterpart of the existing method `SelectionOrderBuilder#seekAfter`.
  * A `SelectionOrder` may filter by victim types, not just victims.
  * Side-by-side and inherited API to select applicable punishments, including historical applicable punishments. The API mirrors the existing selection builder for victim-based targeting, but requires a UUID and NetworkAddress to start working.
  * Both selection methods (by victim, or by applicability) allow defining order of the retrieved punishments.
  * Guarantee the availability of cached mutes when running on appropriate platforms.
* New techniques for adding and updating punishments:
  * Added method to get back a `DraftPunishmentBuilder` from a `DraftPunishment`.
  * Added calculate punishment API based on escalation tracks, along with new events.
  * New methods to update punishment details like the reason, scope, and end date.
* Alt detection is now exposed, meaning API users can fetch alt account information.
* Punishments can be purged completely using appropriate API.
* Expanded API for server scopes.

