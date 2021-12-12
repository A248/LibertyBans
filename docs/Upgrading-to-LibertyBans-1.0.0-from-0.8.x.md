LibertyBans 1.0.0 contains several breaking changes. This page is intended to help you migrate from 0.8.x.

The upgrade should be mostly seamless and core functionality will work out of the box. No manual data migration will be necessary.

## Pre-requisites and Getting Started

To start with, you should be on LibertyBans 0.8.2 before you upgrade to 1.0.0.

## Changes to permissions

Permissions for enacting punishments (banning, muting, warning, kicking) and undoing punishments (unbanning, unmuting, un-warning) follow a more logical pattern, as described on the wiki page: [Permissions](Permissions)

The difference in permissions between 0.8.x and 1.0.0 is more than a simple rename.
  * In 0.8.x, creating an IP-ban requires the permission to IP-ban as well as the permission to ban normally. In 1.0.0, creating an IP-ban only requires the permission to IP-ban.
  * An additional duration permission format was supported on LibertyBans 0.8.x, for backwards compatibility reasons: `libertybans.dur.ban.<timespan>` ('dur' and 'ban' are swapped). Compatibility with this format is removed in 1.0.0; the correct format is `libertybans.ban.dur.<timespan>`.

## Changes to the messages.yml

Some options in the messages.yml configuration have been renamed. To migrate your messages.yml easily, I suggest following this example (sourced from the Russian translation of LibertyBans): 

* The `playerOrAddress` option is renamed to `player-or-address`.
* `additions.<type>.permission.command` is now `additions.<type>.permission.uuid`. Similarly, `removals.<type>.permission.command` is now `removals.<type>.permission.uuid`.
* `banList` is now `ban-list` and `muteList` now `mute-list`.

## Databases - MariaDB and MySQL

### Version Requirements

If you use MariaDB or MySQL, you may need to upgrade your database server:
* If you use MariaDB, at least MariaDB 10.3 is required. MariaDB 10.6 (the latest) is recommended.
* If you use MySQL, at least MySQL 8.0 is required. MySQL 8.0 is the latest version.

From LibertyBans 0.8.1 onward, older database versions are detected and a warning admonishes you in the server console.
  * If you are on an older database version, you should have seen this warning. Nonetheless you ought to check your server console in the case you missed it.

Please keep in mind that other plugins, either on account of software age or inadequacy, may be incompatible with newer database versions. It is your responsibility to ensure compatibility.

### Distinction between MariaDB and MySQL

It is now necessary to distinguish between MariaDB and MySQL when configuring the sql.yml:

* If you use MySQL, change `rdms-vendor` in the sql.yml to `MYSQL`.
* If you use MariaDB, no action is needed.

## Multi-Instance / Multi-Proxy Support

LibertyBans 0.8.2 can co-exist with LibertyBans 1.0.0 on the same database. Before beginning to upgrade to 1.0.0, you must set the option `version1-compatibility-mode` to `true` in the `sql.yml` for all your 0.8.2 LibertyBans instances.

If you forgot to enable `version1-compatibility-mode`, the upgrade will still work, as long as you do not restart the 0.8.2 LibertyBans instances. If you restart a 0.8.2 LibertyBans instance which does not have `version1-compatibility-mode` enabled, the instance will fail to start.

After you upgrade to 1.0.0, it is safe to remove `version1-compatibility-mode` since this option does not exist in 1.0.0

## Non-Breaking Changes

There are some improvements which are non-breaking, but might be useful to know.

* The event scheduler feature (`mariadb.use-event-scheduler` in the sql.yml) has been removed. This feature existed solely as a minor performance optimization. If you were using this feature before, you can drop the event with `DROP EVENT IF EXISTS libertybans_refresher`
