
鉴于LiteBans是闭源收费的，我们很难获取其运作的信息。其内部的代码设计细节几乎不为人所知。事实上，对LiteBans进行逆向工程可能甚至是违法的。

本对比页面将聚焦于对LiteBans已知的信息。

## 设计

### API

LiteBans的API鼓励用户去直接查询它的数据库。因此，和它的API打交道的开发者批评过它的抽象性很差。要求API用户直接执行SQL语句，会把这些用户与数据库模式牢牢绑定在一起。

这便有以下后果：
* 如果附属插件发生了连接泄露，其原因便会归咎到LiteBans上面。由于LiteBans的代码被混淆，随后的调试工作就会变得更复杂。
* API用户高度依赖数据库模式的现状，意味着在不造成大规模破坏性变更的前提下，LiteBans很难优化它的数据库模式。

相反，LibertyBans提供了一个可扩展的Java API，旨在囊括一个处罚插件能够提供的所有功能。相比于LiteBans，使用LibertyBans API的插件不需要直接执行SQL语句。

### 数据库模式

和其他处罚插件一样，LiteBans会在储存UUID和IP地址时使用VARCHAR类型的列。这等效于直接存储字符串格式的UUID和IP地址。

LibertyBans则会使用BINARY类型的列，以此来节约储存空间。这意味着只有存储UUID和IP地址时必须的字节会被储存。实际上的差异很小，但也不容忽视。

### 撤销操作者

LiteBans会记录撤销处罚的管理员信息。这样工作人员可以决定撤销处罚的责任人。

LibertyBans则不会储存这一信息。

## 功能实现

### 测试套件

LibertyBans拥有一套广泛的测试套件。自动的测试可以在发布版本之前找出可能的漏洞。自动测试的范围和深度为LibertyBans省下了无数的开发时间。

LiteBans是闭源的，因此并不清楚其是否具备测试套件。但是，现有证据表明LiteBans并没有完备的自动测试：
* LiteBans曾经出现过一些漏洞，这些漏洞暗示LiteBans没有足够的测试流程：
  * “Fixed the /unwarn command, broken since 2.1”（修复了自2.1以来乌发运作的/unwarn命令）与“Fixed a harmless error when starting the plugin for the first time if config.yml doesn't exist yet”（修复了在首次启动插件时因config.yml不存在抛出的一个无害的错误） - https://www.spigotmc.org/resources/litebans.3715/update?update=102167
  * “Fixed Database.prepareStatement() returning a closed statement”（修复了Database.prepareStatement()返回一个已关闭语句的问题） - https://www.spigotmc.org/resources/litebans.3715/update?update=163048
  * 因无效查询语句导致的一个bug： https://gitlab.com/ruany/LiteBans/-/issues/391
  * 这些类型的bug很可能会通过自动测试被避免。
* 漏洞描述中频繁提及手动测试，但没有一个提到自动测试。

### 平台区分

LibertyBans的代码会被拆分成面向不同平台的代码。

为平台拆分代码可以完全避免意外的类初始化导致的一整类漏洞。漏洞越少，耗在调试问题的时间就越少，提升插件其余部分的时间就越多。

尽管LiteBans是闭源的，现有证据表明它的代码很可能并没有依据平台进行拆分。<sup id="note1ret">[1](#note1)</sup>

## 设计理念

### 价格与可用性

LibertyBans是自由、开源的软件。所有人都可以查看它的源代码。所有人都可以参与其中、为其做出贡献、添加新功能、或者修改后自用。用户随时可以使用最新版本的源代码，无需等到下一次官方发布。

LiteBans则是闭源的。只有作者能够访问源代码，只有作者能够实现新功能。它的jar文件经过混淆，所以不能被反编译成可读的源代码。

此外，LiteBans是收费的，用户必须为了能使用它而付费。这也意味着那些有前瞻性的用户不能轻易地进行测试。他们必须在测试前付费。

Users of LiteBans must carefully consider whether they are willing to rely entirely on the author of LiteBans for features and improvements.

### Support

Support for LibertyBans is provided through GitHub Issues and a dedicated text channel on a Discord Server.

LiteBans primarily offers support through Discord; however, it only does so for users who verified their purchase of the plugin. This also includes requesting support for their API, meaning if you're not a customer of their plugin, chances are low you will gain access to their support server, or receive help with their API at all.

### Versioning

LiteBans does not follow semantic versioning, leading to potential instability for developers using its API. It has introduced new API without issuing a minor release.<sup id="note2ret">[2](#note2)</sup>

LibertyBans fully follows semantic versioning for its API.

## Requirements

### Java Version Support

LibertyBans requires Java 17 whereas LiteBans permits Java 8.

### External Databases

Neither LibertyBans nor LiteBans requires an external database. LibertyBans uses HSQLDB by default, and LiteBans uses H2 by default.

Both LibertyBans and LiteBans support MariaDB, MySQL, and PostgreSQL. LiteBans also has support for SQLite, but SQLite usage is discouraged by LiteBans.

LibertyBans requires certain minimum versions for database servers. At least MySQL 8.0, MariaDB 10.3, or PostgreSQL 12 is required. Older versions are not supported.

## Platform Support

* Bukkit, BungeeCord, and Velocity are supported by both plugins.
* Sponge:
  * LibertyBans supports Sponge.
  * LiteBans declines to support Sponge on the author's reason that Sponge does not provide asynchronous chat events.<sup id="note3ret">[3](#note3)</sup>

LiteBans' support for Velocity came after repeated user requests. It was suggested that LiteBans be made open-source so that someone could contribute a Velocity version. The reluctance of the LiteBans author to add Velocity support suggests that it may be unwise to rely on proprietary software for critical functionality.

## Features

### Geyser Support

LibertyBans and LiteBans have Geyser support.

However, documentation suggests LiteBans requires the prefix to be the period character (".").<sup id="note4ret">[4](#note4)</sup>

### Core punishment types

Both LibertyBans and LiteBans include bans, ip-bans, mutes, warns, and kicks.

By the nature of its flexible design, LibertyBans also supports ip-mutes, ip-warns, and ip-kicks, even though these are rarely used. It costs nothing to add these extra features.

LiteBans allows banning IP ranges. LibertyBans does not.

### Combatting Punishment Evasion (Alt Accounts)

Both LibertyBans and LiteBans have a command to check for alts as well as an automatic notification when an alt of a banned player joins the server.

LibertyBans further supports multiple modes of IP address-based punishment, in order to automatically block alt accounts. LiteBans does not have this feature.

### Exemption

Although both plugins support the exemption feature which prevents staff from banning each other, it is implemented slightly differently.

For offline players, LiteBans's permission checking depends on Vault on single servers. Without a Vault-compatible permissions plugin, the feature breaks silently for offline players.

LibertyBans' exemption feature will never break silently. However, it requires the installation of a supported exemption provider - currently LuckPerms or Vault. Without an exemption provider, the feature is entirely unavailable.

Moreover, LiteBans' exemption does not provide arbitrary levels.<sup id="note5ret">[5](#note5)</sup>

### Importing From Other Plugins

LibertyBans supports importing from AdvancedBan, BanManager, LiteBans, and vanilla.

LiteBans supports importing from AdvancedBan, BanManager, BungeeAdminTools, MaxBans, UltraBans, and vanilla.

### Server Scopes

Both plugins enable punishments scoped to certain servers.

However, LiteBans lacks scope categories, or the ability to group servers into a single scope.<sup id="note6ret">[6](#note6)</sup>

### Multi-Proxy / Multi-Instance Synchronization

Both LibertyBans and LiteBans provide synchronization across multiple instances, a feature commonly used for multi-proxy setups.

## References

<a id="note1">1</a>: Falistos. "Start error under Java 18." LiteBans Gitlab Issue. https://gitlab.com/ruany/LiteBans/-/issues/408 [↩](#note1ret)

<a id="note2">2</a>: Ruan. "LiteBans 2.5.4 - 2.5.9." SpigotMC Resource Update. https://www.spigotmc.org/resources/litebans.3715/update?update=341296 [↩](#note2ret)

<a id="note3">3</a>: Ruan. "[Feature] Spongepowered?". LiteBans Gitlab Issue Comment. https://gitlab.com/ruany/LiteBans/-/issues/41#note_324182783 [↩](#note3ret)

<a id="note4">4</a>: Ruan. "LiteBans 2.7.5 (Connection error fix)". SpigotMC Resource Update. https://www.spigotmc.org/resources/litebans.3715/update?update=414133 [↩](#note4ret)

<a id="note5">5</a>: Ruan. "LiteBans Exempt". LiteBans Gitlab Issue Comment. https://gitlab.com/ruany/LiteBans/-/issues/223 [↩](#note5ret)

<a id="note6">6</a>: lewisakura. "Server scope groups". LiteBans Gitlab Issue. https://gitlab.com/ruany/LiteBans/-/issues/452 [↩](#note6ret)

### Disclaimer

Please note that no harm is meant to subjects of criticism. If the writing sounds harsh, we apologize; please let us know and we will make the language less harsh.
