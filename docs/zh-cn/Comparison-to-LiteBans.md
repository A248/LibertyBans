
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
  * “Fixed the /unwarn command, broken since 2.1”（修复了自2.1以来无法运作的/unwarn命令）与“Fixed a harmless error when starting the plugin for the first time if config.yml doesn't exist yet”（修复了在首次启动插件时因config.yml不存在抛出的一个无害的错误） - https://www.spigotmc.org/resources/litebans.3715/update?update=102167
  * “Fixed Database.prepareStatement() returning a closed statement”（修复了Database.prepareStatement()会返回一个已关闭语句的问题） - https://www.spigotmc.org/resources/litebans.3715/update?update=163048
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

LiteBans的用户应当谨慎考虑他们是否愿意完全仰仗于LiteBans的作者来添加新功能和改进。

### 支持

LibertyBans的支持通道位于Github Issues和Discord频道上一个独立的文本频道。

LiteBans主要通过Discord提供支持；但是他们只会为已购买插件并通过验证的用户提供支持。这也包括请求与API相关的支持。这意味着如果您不是该插件的客户，您不太可能能够访问他们的支持通道，或者取得与其API相关的支持。

### 版本控制

LiteBans并不遵守语义化版本控制，这为使用其API的用户带来了潜在的不稳定性。它已经有过引入新API功能，但不发布新的次版本号的先例。<sup id="note2ret">[2](#note2)</sup>

LibertyBans的API完全遵守语义化版本控制。

## 依赖

### Java版本支持

LibertyBans需要至少Java 17，而LiteBans仅需Java 8。

### 外部数据库

外部数据库对LibertyBans和LiteBans均不是必需的。LibertyBans默认使用HSQLDB数据库，而LiteBans默认使用H2数据库。

LibertyBans和LiteBans均支持MariaDB、MySQL、和PostgreSQL。LiteBans还支持SQLite，但是他们并不推荐使用它。

LibertyBans对部分数据库服务器有最低版本要求。MySQL需至少8.0，MariaDB需至少10.3，而PostgreSQL需至少12。旧版本均不受支持。

## 平台支持

* 两个插件均支持Bukkit，Bungeecord，和Velocity。
* 关于Sponge：
  * LibertyBans支持Sponge.
  * LiteBans拒绝支持Sponge，作者给出的原因是Sponge不提供异步聊天事件。<sup id="note3ret">[3](#note3)</sup>

LiteBans对Velocity的支持是在用户反复提出功能请求后才添加的。有人提出LiteBans应该开源，这样就有人能贡献一个Velocity版本。LiteBans作者添加Velocity的迟滞性表明，依赖闭源软件实现关键功能恐怕是不明智的。

## 功能

### Geyser支持

LibertyBans和LiteBans均提供Geyser支持。

但是，LiteBans的文档表明Geyser的用户名前缀必须是点（"."）。<sup id="note4ret">[4](#note4)</sup>

### 核心处罚类型

LibertyBans和LiteBans均提供封禁、IP封禁、禁烟、警告和踢出功能。

得益于LibertyBans的灵活设计，它也支持对IP地址进行禁言、警告或踢出（虽说很少被使用）。添加这些功能没有任何额外开销。

LiteBans支持封禁IP地址范围，而LibertyBans不支持。

### 对抗处罚逃避（应对小号）

LibertyBans和LiteBans都有查询小号的命令，也都能在被封禁用户的小号进入服务器时自动发出提示。

LibertyBans还支持多种基于IP地址的处罚严格等级，以此自动封禁小号。LiteBans则不存在此功能。

### 豁免功能

尽管两个插件都支持阻止管理员互相封禁的豁免功能，它们各自的实现有细微的差异。

对于离线玩家，单服务器上LiteBans的权限检查依赖于Vault权限节点（单服务器）。如果没有安装与Vault兼容的权限插件，离线玩家的权限检测会静默地被跳过。

LibertyBans的豁免功能不会被静默地跳过。不过，这就会导致更多的依赖项。LibertyBans要求安装支持的豁免提供者——目前是LuckPerms或者Vault。如果提供者不存在，该功能完全不可用。

此外，LiteBans的豁免功能并没有数字等级。<sup id="note5ret">[5](#note5)</sup>

### 从其他插件导入

LibertyBans支持从AdvancedBan、BanManager、LiteBans、和原版服务器导入数据。

LiteBans支持从AdvancedBan、BanManager、BungeeAdminTools、MaxBans、UltraBans、和原版服务器导入数据。

### 服务器范围

两个插件都可以执行针对特定服务器的处罚。

不过，LiteBans没有服务器分类功能，即把多个服务器组合为一个范围的功能。<sup id="note6ret">[6](#note6)</sup>

### 多代理/多实例同步

LibertyBans和LiteBans都会在多个实例之间同步处罚记录。这一般用于多个代理的配置。

## 参考资料

<a id="note1">1</a>: Falistos. "Start error under Java 18." LiteBans Gitlab Issue. https://gitlab.com/ruany/LiteBans/-/issues/408 [↩](#note1ret)

<a id="note2">2</a>: Ruan. "LiteBans 2.5.4 - 2.5.9." SpigotMC Resource Update. https://www.spigotmc.org/resources/litebans.3715/update?update=341296 [↩](#note2ret)

<a id="note3">3</a>: Ruan. "[Feature] Spongepowered?". LiteBans Gitlab Issue Comment. https://gitlab.com/ruany/LiteBans/-/issues/41#note_324182783 [↩](#note3ret)

<a id="note4">4</a>: Ruan. "LiteBans 2.7.5 (Connection error fix)". SpigotMC Resource Update. https://www.spigotmc.org/resources/litebans.3715/update?update=414133 [↩](#note4ret)

<a id="note5">5</a>: Ruan. "LiteBans Exempt". LiteBans Gitlab Issue Comment. https://gitlab.com/ruany/LiteBans/-/issues/223 [↩](#note5ret)

<a id="note6">6</a>: lewisakura. "Server scope groups". LiteBans Gitlab Issue. https://gitlab.com/ruany/LiteBans/-/issues/452 [↩](#note6ret)

### 免责声明

请注意，本文没有任何贬损其中插件主体的目的。如有冒犯，我们深表歉意；请联系我们，我们会修改其措辞。
