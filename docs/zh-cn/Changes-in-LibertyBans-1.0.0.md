
本页面是LibertyBans在1.0.0版本所更新的正式日志。该页面包含更新的各个方面，包括破坏性更新内容、鲜有提及且用户察觉不到的技术性调整、各方面增加的功能和对新功能通透的描述、还有设计和结构上的决定以及丰富的小细节。

如果您需要从LibertyBans 0.8.x版本升级到1.0.0版本的指南，请查阅[此页面](Upgrading-to-LibertyBans-1.0.0-from-0.8.x)。这个指南聚焦于破坏性更新以及它们对您的影响。

## 更改日志

* 权限节点被重构了，现在它们遵循一种逻辑自洽的规则。
* 权限的消息已通过`PunishmentPermissionSection`重构。在每种处罚类型的记录增减中，都会统一使用这个组件。
* 放弃了对LibertyBans 0.7.6版本中存在漏洞的处罚时间限制权限节点格式的支持。
* 不再支持MariaDB 10.2和MySQL 5.7。
* message.yml中的`playerOrAddress`选项改名为`player-or-address`，多处的`permission.command`改名为`permission.uuid`，同时`banList`和`muteList`也分别改名为`ban-list`和`mute-list`。
* 1.0.0版本中重写了数据库模式，因此0.8.2等老版本将无法使用新版本的数据库。为确保现有数据被无缝转移，1.0.0版本中将添加一个基于Java的Flyway迁移模块，用于识别现有的0.8.2数据表并将它们中的数据转移到新表中。该迁移模块将在1.1.0版本中被移除，并且基于Java的迁移过程在校验数据时的行为会和普通的Flyway迁移过程有所差异。
* 所有的SQL查询语句均已使用JOOQ重写。插件构建中将运行JOOQ的代码生成。这一更改实现了对PostgreSQL的兼容，因为以前的查询使用的是反向标记。
* 集成测试现在在MariaDB实例之外还会会启动PostgreSQL实例。
* Flyway已更新到8.x版本。
* MySQL和MariaDB中的事件计划功能已被移除。该功能带来的好处与其维护成本无法匹配。
* Added accepted value 'MYSQL' for `rdms-vendor` and require MySQL to be distinguished from MariaDB.
* LibertyBans now handles transaction serialization failure and will retry transactions which failed due to contention:
  * Serialization failure describes the situation where multiple database transactions operate on the same data, and somehow conflict with one another. Serialization failures are propagated to the application.
  * Whether software handles serialization failure has nothing to do with whether it maintains the integrity of its data: it is not possible to corrupt data merely due to a serialization failure.
  * This means, in rare cases where running a command would result in a serialization failure, LibertyBans 1.0.0 will handle the situation gracefully, whereas LibertyBans 0.8.x will fail with an exception. Symptoms in 0.8.x would include stacktraces in the server console. However, in practice, no one has reported any instance of serialization failure in LibertyBans.
  * The handling of transaction serialization failure will not be back-ported to LibertyBans 0.8.x. Much software does not handle transaction serialization failure (including LibertyBans 0.8.x) therefore this is not considered a bug of sufficient importance.
  * Further technical reading: https://stackoverflow.com/questions/7705273/what-are-the-conditions-for-encountering-a-serialization-failure
* Relevant client encoding variables are set per each database upon establishing connection. Also, the charset utf8mb4 and collation utf8mb4_bin are now set on created tables for MariaDB and MySQL.
* 添加了组合式处罚对象类型。这类处罚目标同时包含一个UUID和IP地址。
  * 如果您希望默认执行基于IP的处罚，又不想纠结于那些和IP地址相关的技术性内容，那么您适合使用组合式处罚目标。换句话来讲，组合式处罚适用于那些希望将玩家处罚按IP处罚对待，又想要仅通过玩家名而非IP地址来执行处罚、撤销处罚、或列出记录的用户。
  * 请查阅组合式处罚的wiki页面了解更多细节。
* /accounthistory命令现在接受`-both`参数，以此来显示匹配指定玩家UUID或IP地址的所有玩家。
* 调整了HikariCP在发布版中的路径。
* 为处罚消息添加了新的变量：
  * %TYPE_VERB% - 处罚类型的动词形式
  * %TIME_PASSED_SIMPLE% - 与TIME_PASSED类似，但是会四舍五入到最大时间单位
  * %TIME_REMAINING_SIMPLE% - 与TIME_REMAINING类似，但是会四舍五入到最大时间单位
  * %HAS_EXPIRED% - 处罚是否已经超出规定的时间并因此过期
* 添加了%PREVIOUSPAGE%变量。

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
