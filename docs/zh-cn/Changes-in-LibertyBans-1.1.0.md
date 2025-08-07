
## Changes

* Require Java 17.
* Using MariaDB with LibertyBans now requires MariaDB 10.6 at minimum.
* Automatic detection of Geyser/Floodgate and determination of the Bedrock name prefix.
  * The `GEYSER` server-type option has been removed. This server type was inflexible; it failed to distinguish between online and offline Geyser-utilizing servers. It is obsolete due to automatic Floodgate detection.
  * The name prefix is queried from the Floodgate API at startup.
* Before this release, `STRICT` address-strictness enforced user punishments too stringently - as if they were IP-based punishments - which was contrary to the documentation.
  * Because it is impossible to retroactively change the behavior of `STRICT` without breaking existing setups, a new address-strictness setting `STERN` is added. `STERN` behaves the way `STRICT` was previously documented to.
  * The `STRICT` documentation has updated to reflect its full behavior.
  * See the wiki page for more information.
* Added the exemption feature. To support exemption flexibly, different exemption backends are offered:
  * The `exemption-luckperms` addon depends on LuckPerms and uses LuckPerms' group weights to define the exemption hierarchy.
  * The `exemption-vault` addon depends on Vault and works only on Bukkit. It uses tiered permissions to define exemption levels.
  * If their dependencies are not met, or if they are installed on the wrong platform, these addons will not load.
  * See the wiki for documentation on this feature.
* In the rare case that multiple mutes are issued very quickly, the `warn-actions` addon will make sure warns do not "overlap" one another. There were no reports of this happening, but it was a theoretical possibility. The solution works by leveraging the newly-added `seekBefore` API.
* Snapshot versions are now differentiated according to the build timestamp.
* The database-related thread pool is now fully shut down and its termination awaited before the connection pool is closed. This prevents a harmless exception which occurred when shutdown coincided with the periodic synchronization task.

### API Changes

* Expand selection capabilities:
  * `SelectionOrder#countNumberOfPunishments` yields the pure number of punishments, which is more efficient than retrieving the punishments themselves from the database.
  * `SelectionOrderBuilder#seekBefore` allows retrieving punishments before a specified time / ID. It is the counterpart of the existing method `SelectionOrderBuilder#seekAfter`.
  * A selection order may filter by victim types, not just victims.
