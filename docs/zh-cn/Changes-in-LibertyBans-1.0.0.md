
本页面是LibertyBans在1.0.0版本所更新的正式日志。该页面包含更新的各个方面，包括破坏性更新内容、鲜有提及且用户察觉不到的技术性调整、各方面增加的功能和对新功能通透的描述、还有设计和结构上的决定以及丰富的小细节。

如果您需要从LibertyBans 0.8.x版本升级到1.0.0版本的指南，请查阅[此页面](zh-cn/Upgrading-to-LibertyBans-1.0.0-from-0.8.x)。这个指南聚焦于破坏性更新以及它们对您的影响。

## 更新内容

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
* `rdms-vendor`现在接受“MYSQL”一值，同时也需要显式区分MySQL和MariaDB。
* LibertyBans现在会妥当处理操作异常，并且会在出现操作冲突时进行重试：
  * 序列化异常指的是多个数据库实例对同一数据进行操作时发生冲突的情况。序列化异常会传播到应用程序中。
  * 软件是否处理序列化异常与它能否保持数据完整性无关：仅凭序列化异常不可能导致数据损坏。
  * 这意味着，在极少数情况下，当执行命令导致序列化异常时，LibertyBans 1.0.0将妥当地处理该情况，而LibertyBans 0.8.x则会中断操作并抛出异常。0.8.x版本中的异常现象包括服务器控制台显示的堆栈跟踪信息。不过实际上，目前尚未有人报告LibertyBans出现过任何序列化异常的案例。
  * 处理序列化异常的操作不会向后移植到0.8.x版本。很多软件（包括LibertyBans 0.8.x）都不会处理序列化异常，因此这不会被视作一个有足够严重性的漏洞。
  * 了解进一步的技术细节：https://stackoverflow.com/questions/7705273/what-are-the-conditions-for-encountering-a-serialization-failure
* 建立连接时，会为每个数据库设置相应的客户端编码变量。此外，现在在创建MariaDB和MySQL数据库时，会默认为表设置utf8mb4字符集和utf8mb4_bin排序规则。
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

### API 变更

* API现在为处罚ID使用`long`类型（64位整数）。适配更新时请将其转换为long类型，或重构代码，使用long类型的ID。
  * `getID`方法已被弃用，但是为兼容性而保留。请尽可能改用`getIdentifier`方法。
* `EnforcementOptions`现已加入API，并替换了DraftPunishment、Punishment、和RevocationOrder中的部分方法。如果您使用了任意一种'withoutEnforcement'或'withoutUnenforcement'方法，您需要修改您的代码。您可按照下方列表进行替换：
  * 对于DraftPunishment: `draftPunishment.enactPunishmentWithoutEnforcement()` -> `draftPunishment.enactPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build())`
  * 对于Punishment:
    * `punishment.undoPunishmentWithoutUnenforcement()` -> `punishment.undoPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build)`
    * `punishment.unenforcePunishment()` -> `punishment.unenforcePunishment`
  * 对于RevocationOrder:
    * `revocationOrder.undoPunishmentWithoutUnenforcement()` -> `revocationOrder.undoPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build())`
    * `revocationOrder.undoAndGetPunishmentWithoutUnenforcement()` -> `revocationOrder.undoAndGetPunishment(EnforcementOptions.builder().enforcement(Enforcement.NONE).build())`
* `space.arim.libertybans.api.revoke`包已被合并到`space.arim.libertybans.api.punish`包中。Import语句也需要对应地更新。
* 选择处罚记录的API的功能已得到扩展。现支持分页操作。由于此次变更，`SelectionOrderBuilder`和`SelectionOrder`出现了破坏性更新：
  * `SelectionOrderBuilder#maximumToRetrieve`已被移除，取而代之的是和SQL中的LIMIT等效的`#limitToRetrieve`。
* `PunishmentRevoker`有非破坏性的更新：
  * 对于`PunishmentRevoker#revokeByTypeAndVictim`方法，取消了惩罚类型必须为单数的要求（即不再需要调用PunishmentType.isSingular()）。不过，如果提供了非单个的处罚类型，取消处罚的行为将不再可控。
  * 添加了方法`PunishmentRevoker#revokeByTypeAndPossibleVictims`。
* 其他破坏性更新只会在您自行实现API时造成影响（这不太可能）：
  * `SelectionOrder` 上的Getter方法体现了新增和扩展的 API。
  * `RevocationOrder` 上的Getter方法体现了扩展的 API。
* 向API中加入了Punishment.PERMANENT_END_DATE常量。
* 现在可以通过API执行“静默”处罚。
* 添加了`PunishmentFormatter#formatPunishmentTypeVerb`。
