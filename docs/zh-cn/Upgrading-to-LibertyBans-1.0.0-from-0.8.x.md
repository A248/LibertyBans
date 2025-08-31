
LibertyBans 1.0.0版本包含了若干个破坏性更新。本页面将帮助您从0.8.x版本迁移至新版本。

大多数更新内容应当是无感的，核心功能应该能立刻投入使用。没有必需的手动数据迁移操作。

## 准备开始

在更新到LibertyBans 1.0.0之前，您应当使用最新的0.8.x版本——目前是0.8.2。

# 手动操作

对于部分更新，您需要手动调整您的服务器配置。

## 权限的调整

执行处罚（如封禁、禁言、警告、踢出等）和撤销处罚的权限节点现在遵循一种更符合逻辑的格式，请查阅wiki页面：[权限](Permissions)。

0.8.x和1.0.0之前权限的差异不只是简单的重命名。
  * 在0.8.x版本中，执行IP处罚不仅需要处罚IP的权限，还需要执行常规处罚的权限。在1.0.0版本中，执行IP处罚只需要处罚IP的权限。
  * 为兼容性考虑，LibertyBans 0.8.x版本支持一个额外的时限权限节点`libertybans.dur.ban.<时间范围>`（即`dur`和`ban`对调了）。1.0.0版本不再支持此格式，正确的格式是`libertybans.ban.dur.<时间范围>`。

## Changes to the messages.yml

Some options in the messages.yml configuration have been renamed. To migrate your messages.yml easily, I suggest following this example (sourced from the Russian translation of LibertyBans): 

* The `playerOrAddress` option is renamed to `player-or-address`.
* `additions.<type>.permission.command` is now `additions.<type>.permission.uuid`. Similarly, `removals.<type>.permission.command` is now `removals.<type>.permission.uuid`.
* `banList` is now `ban-list` and `muteList` now `mute-list`.

## Databases - MariaDB and MySQL

### Version Requirements

If you use MariaDB or MySQL, you may need to upgrade your database server:
* If you use MariaDB, at least MariaDB 10.3 is required. The latest MariaDB version is recommended.
* If you use MySQL, at least MySQL 8.0 is required. MySQL 8.0 is the latest version.

From LibertyBans 0.8.1 onward, older database versions are detected and a warning admonishes you in the server console.
  * If you are on an older database version, you should have seen this warning. Nonetheless you ought to check your server console in the case you missed it.

Please keep in mind that other plugins, either on account of software age or inadequacy, may be incompatible with newer database versions. It is your responsibility to ensure compatibility.

### Distinction between MariaDB and MySQL

It is now necessary to distinguish between MariaDB and MySQL when configuring the sql.yml:

* If you use MySQL, change `rdms-vendor` in the sql.yml to `MYSQL`.
* If you use MariaDB, no action is needed.

## Multi-Instance / Multi-Proxy Support

This documentation is intended for LibertyBans 0.8.2, an upcoming 0.8.x release which will add a compatibility mode.

The compatibility mode will allow you to run LibertyBans 0.8.2 on the same database as 1.0.x.

Until this compatibility mode is released, it is not possible to use LibertyBans 0.8.x and 1.0.x side-by-side on the same database. Therefore, you should upgrade your multi-proxy network once 0.8.2 has been made available.

~~LibertyBans 0.8.2 can co-exist with LibertyBans 1.0.0 on the same database. Before beginning to upgrade to 1.0.0, you must set the option `version1-compatibility-mode` to `true` in the `sql.yml` for all your 0.8.2 LibertyBans instances.~~

~~After you upgrade to 1.0.0, it is safe to remove `version1-compatibility-mode` since this option does not exist in 1.0.0~~

# Automatic Action

## Database Migration

LibertyBans 1.0.0, when it starts, will automatically upgrade your database to 1.0.0.

The database schema has breaking changes, and some software which relies on it, such as the NamelessMC punishment panel, may need to be updated.

## Configuration Migration

New configuration options will be added automatically.

Existing configuration options will be preserved.

# Other useful information

## Non-Breaking Changes

These changes might be useful to know.

* The event scheduler feature (`mariadb.use-event-scheduler` in the sql.yml) has been removed. This feature existed solely as a minor performance optimization. If you were using this feature before, you can drop the event with `DROP EVENT IF EXISTS libertybans_refresher`

## Automatic Upgrade from 0.8.x

LibertyBans will not delete the 0.8.x data. If you'd like to free up disk space, you can remove the old tables.
  * Following a successful import, the 0.8.x tables will have "zeroeight_postmigration" in their name.
  * **Please be careful with your data**. Always take a backup before deleting large amounts of data.
  * In HSQLDB, you would need to edit the hypersql/punishments-database.script file. This must be done while the server is stopped. Deleting the tables correctly is not trivial, so it is suggested to ask for support if necessary.
