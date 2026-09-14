
### This page is not complete, and it will continue to receive updates until 1.2.0 is fully released.

## Changes

* Velocity 4 and Fabric are now supported platforms.
* For Sponge, API 15 and upwards is a minimum requirement, while **Sponge API 13 and below** are unsupported.
* The LibertyBans API is now published to Maven Central.
  * Use `org.libertybans:bans-api:1.2.0-M1`, for example.
  * Developers should remove all usages of the old repository, `mvn-repo.arim.space`. This repository may be retired in a couple years.
* Server scope values can now be longer. The old limit was 32, the new limit is 255 characters.

### Features

* Duration units can be chained and combined to support more lengths.
  * For example, `/ban A248 12h30min banned for 12.5 hours`.
* Support IPv6 command arguments
* Whitelist of IP addresses managed by `/libertybans ip-records whitelist`:
  * Must be enabled in the config.yml.
  * Implemented on a best-effort basis, but not all methods of detection (e.g., STRICT address enforcement with intermediary IPs) can use the whitelist.
* Purge an IP address from the database with `/libertybans ip-records purge`

### API Changes

* Exposed silent status in `PostPunishEvent`/`PostPardonEvent`.
* Added `PostOpNotificationEvent` as a supertype of the post-punish and post-pardon events.
* More to come... this page is not yet up-to-date.

