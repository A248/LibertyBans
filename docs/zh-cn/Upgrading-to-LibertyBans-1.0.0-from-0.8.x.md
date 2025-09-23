
LibertyBans 1.0.0版本包含了若干个破坏性更新。本页面将帮助您从0.8.x版本迁移至新版本。

大多数更新内容应当是无感的，核心功能应该能立刻投入使用。没有必需的手动数据迁移操作。

## 准备开始

在更新到LibertyBans 1.0.0之前，您应当使用最新的0.8.x版本——目前是0.8.2。

# 手动操作

对于部分更新，您需要手动调整您的服务器配置。

## 权限的调整

执行处罚（如封禁、禁言、警告、踢出等）和撤销处罚的权限节点现在遵循一种更符合逻辑的格式，请查阅wiki页面：[权限](zh-cn/Permissions)。

0.8.x和1.0.0之前权限的差异不只是简单的重命名。
  * 在0.8.x版本中，执行IP处罚不仅需要处罚IP的权限，还需要执行常规处罚的权限。在1.0.0版本中，执行IP处罚只需要处罚IP的权限。
  * 为兼容性考虑，LibertyBans 0.8.x版本支持一个额外的时限权限节点`libertybans.dur.ban.<时间范围>`（即`dur`和`ban`对调了）。1.0.0版本不再支持此格式，正确的格式是`libertybans.ban.dur.<时间范围>`。

## messages.yml中的调整

message.yml中部分选项被重命名了。为了简单地迁移message.yml，我建议遵循下面这个流程（来自LibertyBans的俄语翻译）：

* `playerOrAddress`选项已被重命名为`player-or-address`。
* `additions.<type>.permission.command`已改为`additions.<type>.permission.uuid`。类似地，`removals.<type>.permission.command` 也改为`removals.<type>.permission.uuid`。
* `banList`已改为`ban-list`，且`muteList`已改为`mute-list`.

## 数据库 - MariaDB与MySQL

### 版本要求

如果您使用MariaDB或MySQL，您可能需要升级您的数据库服务器：
* 如果您使用MariaDB，最低版本要求为MariaDB 10.3。建议使用最新版的MariaDB。
* 如果您使用MySQL，最低要求为MySQL 8.0。它也正是最新的版本。

自LibertyBans 0.8.1起，插件会检测数据库是否过时，并在检测到时在控制台中向您发送警告消息。
  * 如果您使用的是旧版本数据库，您应该已经看到过这条警告消息。无论如何，我们都建议您检查一下控制台，以防您漏过了它。

请注意其他插件可能因软件无人维护或功能不足，与新版本数据库不兼容。处理兼容性的事情是您的责任。

### 显式区分MariaDB和MySQL

现在在配置sql.yml中，需要显式区分MariaDB和MySQL：

* 如果您使用MySQL，请在sql.yml中把`rdms-vendor`设置为`MYSQL`。
* 如果您使用MariaDB，无需任何额外操作。

## 多实例/多代理支持

本段文档是为LibertyBans 0.8.2版本服务的，这个即将发布的0.8.x版本将添加一个兼容模式。

兼容模式使得您可以让LibertyBans 0.8.2版本和1.0.x版本同时使用相同的数据库。

在兼容模式发布之前，您无法让LibertyBans 0.8.x版本与1.0.x版本共用数据库。因此，您应当在0.8.2版本发布后尽快升级您的群组服。

~~LibertyBans 0.8.2 can co-exist with LibertyBans 1.0.0 on the same database. Before beginning to upgrade to 1.0.0, you must set the option `version1-compatibility-mode` to `true` in the `sql.yml` for all your 0.8.2 LibertyBans instances.~~

~~After you upgrade to 1.0.0, it is safe to remove `version1-compatibility-mode` since this option does not exist in 1.0.0~~

# 自动操作

## 数据库迁移

LibertyBans 1.0.0版本启动时，会自动把您的数据库升级到1.0.0版本。

升级后的数据库模式存在破坏性变更，因此部分依赖该数据库的软件，如NamelessMC的处罚面板，也需要相应更新。

## 配置迁移

新配置项会被自动添加。

现有的配置项会被保留。

# 其他实用信息

## 非破坏性变更

这些修改信息可能会比较实用。

* 事件计划的功能（sql.yml中的`mariadb.use-event-scheduler`）已被移除。此前该功能仅用于细微的性能优化。如果您先前使用过这个功能，您可以使用`DROP EVENT IF EXISTS libertybans_refresher`删除这些事件。

## 从0.8.x的自动更新

LibertyBans不会删除0.8.x的数据。如果您想要释放磁盘空间，您可以移除旧数据表。
  * 成功迁移数据后，0.8.x的数据表名称中会含有“zeroeight_postmigration”字样。
  * **数据无价，谨慎操作。**永远不要忘记在大量删除数据之前做好备份。
  * 在HSQLDB中，您需要编辑hypersql/punishments-database.script文件。该操作只能在服务器关闭时进行。在这里正确地删除数据表并非轻而易举，所以必要时我们建议您来寻求支持。
