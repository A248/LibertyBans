
本页面旨在对AdvancedBan和LibertyBans之间在API、设计、和功能实现方面进行客观地分析。

我（A248）既是LibertyBans的创建者，也是AdvancedBan中的一个主要贡献者。尽管我不能做到完全不偏不倚，我能够依赖对两个插件的代码结构来进行深入的分析。

## 设计

### 单例管理

AdvancedBan的代码高度侧重于单例管理。这个设计思路使得其初始化过程非常脆弱。过去曾出现过其他插件引用AdvancedBan的类并过早初始化它们所导致的问题。<sup id="note1ret">[1](#note1)</sup>

LibertyBans完全基于实例进行设计，这意味着其他引用其类的插件不会导致初始化冲突。

### ID

AdvancedBan有一个奇怪的行为：它会为每个处罚记录创建两个ID：一个用于正在生效的处罚记录，另一个用于处罚历史记录之中。

这个机制很反直觉。使用两个不同的ID会在配置申诉系统和消息变量时产生误会。

这个现象实在是太出人意料，甚至有些AdvancedBan的核心贡献者都对此一无所知，并且在了解到的时候十分惊讶。<sup id="note2ret">[2](#note2)</sup>

### 管理员UUID

AdvancedBan只会存储管理员的名称，而不是UUID。如果一个管理员修改了他的名字，AdvancedBan仍会在处罚记录中显示它的旧名。

LibertyBans会储存管理员的UUID，并展示最新的名称。<sup id="note3ret">[3](#note3)</sup>

### 数据库模式

AdvancedBan在存储UUID和IP地址时会使用VARCHAR类型的列。这等效于在数据库里面储存字符串类型的UUID和IP地址。相反，LibertyBans会使用BINARY类型列存储数据，以此缩减存储体积。二者实际差异很小，但也不能忽视。

### 时区

AdvancedBan会在数据库中会存储时区差异，作为时间戳的一部分。这个设计略显臃肿，需要AdvancedBan API的用户谨慎地适应这个未被记录的行为。

LibertyBans会在数据库里面记录UTC时间。在展示处罚记录时，它会将其转换为所配置的时区。

因此，如果您在LibertyBans里面修改了时区，所有的处罚记录都会自动更新它们的显示时间。但在AdvancedBan中，时间就不会更新到新时区里面。

## 功能实现

### 缓存

AdvancedBan采用手动构建的处罚缓存机制。该缓存机制相当脆弱，已导致多个漏洞。其数据填充与失效操作均需人工执行。用户偶尔会报告处罚缓存未能失效的问题。

LibertyBans只会缓存禁言处罚。它会使用一个缓存库。截止到2021年12月21日，没有人反馈因LibertyBans的缓存机制所导致的漏洞。

### 操作一致性

与LibertyBans不同，AdvancedBan不会依赖操作一致性原则。在实践中，这意味着AdvancedBan的部分条件可以在特定情形中被突破——这是AdvancedBan设计中的漏洞，并且不能轻易修复。

在AdvancedBan和LibertyBans中，尝试封禁一个一封禁的玩家都会产生一条错误消息。如果您尝试二次封禁一个玩家，AdvancedBan会告诉您“该玩家已被封禁”。但是，这种条件不会被100%遵守。如果您设法能够以非常快的速度执行/ban命令两次，您就能成功地封禁同一个玩家两次。<sup id="note4ret">[4](#note4)</sup>

尽管听起来快速封禁玩家两次很难实现，但是实际环境中该问题已经屡次出现。自动化处罚机制，如反作弊，不受人类输入速度的限制，因此可以以非常快的速度执行封禁命令。

AdvancedBan中的这个漏洞曾经导致LibertyBans用户在导入AdvancedBan的数据时，发现历史记录中出现了重复的处罚记录。与AdvancedBan不同，LibertyBans充分利用了操作一致性原则，因此不会出现上文所述的AdvancedBan中的漏洞。

### 同步与异步构造

与常识相反，线程的开销并不小。目前Java中的线程是和操作系统的线程一一对应的，这使其需要消耗相当多的资源。好的程序设计会避免在细微的工作上切换上下文环境，不然多线程执行的开销会超过使用多核带来的收益。

AdvancedBan的内部和API都是同步设计的。因此，AdvancedBan的大多数操作都会产生一个新的线程。这就会导致频繁切换上下文，并消耗更多的资源。

LibertyBan的异步设计有一点点优势。线程只会在必要时才会创建，并且会和工作相对应。这样的设计更轻量，也更可控。

另一个后果就是AdvancedBan的开发者更容易在不经意间写出漏洞。更准确来说，开发者更容易在主线程上执行高开销的任务。比如，AdvancedBan会在有限的情况下在主线程上执行查询任务。<sup id="note5ret">[5](#note5)</sup>

注意，本段中的*同步*，**不表示**“在主线程上执行”；同时异步也**不表示**多线程设计。我推荐阅读一下[这份StackOverflow上的答案](https://stackoverflow.com/a/748235/)。

### 线程安全性

AdvancedBan中的部分代码无法做到线程安全。该问题导致了许多至今仍在烦扰AdvancedBan的漏洞。<sup id="note6ret">[6](#note6)</sup>此外，AdvancedBan也不会太重视同步操作。<sup id="note7ret">[7](#note7)</sup>

在LibertyBans中，目前没有任何线程同步问题。

### 连接池

自2.1.9版本开始，AdvancedBan依赖于HikariCP，所以它看起来会使用连接池。但实际上，AdvancedBan内部的同步设计阻止它利用额外连接的优势。很不幸的是，AdvancedBan很难轻易地修复这个问题。那个同步设计也正是保护其他线程不产生安全问题的关键。<sup id="note8ret">[8](#note8)</sup>

LibertyBans使用HikariCP，并且能充分利用其优势。

### UTF-8编码

AdvancedBan过去曾经饱受UTF-8支持的困扰，并且目前的AdvancedBan代码仍然会在部分地方使用默认的编码，这意味着还有潜在的问题。UTF-8编码问题一直是配置文件和数据库定义的一大难题。参考资料：
 * https://www.spigotmc.org/resources/advancedban.8695/update?update=327430 
 * https://www.spigotmc.org/resources/advancedban.8695/update?update=228946
 * https://github.com/DevLeoko/AdvancedBan/issues/433
 * https://github.com/DevLeoko/AdvancedBan/issues/430
 * https://github.com/DevLeoko/AdvancedBan/issues/202
 * https://github.com/DevLeoko/AdvancedBan/issues/74

LibertyBans在设计时会刻意的完全支持Unicode编码。

### API文档

AdvancedBan API的文档很不充分。这使得其他使用AdvancedBan API的开发者不得不花费大量时间，通过查看实际实现代码或者寻求支持来探索API方法的含义和细节。诸如空值性和方法约定的细节在AdvancedBan API的文档中只字未提。

AdvancedBan不明确的API描述也会导致不正确的假定：外部代码由于高度依赖特定的实现细节，很可能会依赖AdvancedBan作者非预期中的行为来运作。这样一来，AdvancedBan未来的更新可能会意外地破坏依赖特定内部行为的附属插件。

相反，LibertyBans API通过javadoc进行了详尽的说明。这使得其他开发者很容易学习并使用LibertyBans的API。方法预期行为、约定、和保证的细节都有明确的表述和解释。

## 设计理念

### 版本命名规范

LibertyBans严格遵守语义化版本控制。这意味着在相同主版本号内更新（比如1.x，或2.x）不会导致破坏性更新。语义化版本空值使得附属插件的开发更加容易，因为其兼容性能够通过一个明确的数字来表明。这意味着在相同主版本号内进行更新不会破坏现有的配置。

AdvancedBan并不遵守语义化版本控制。相反，它会在次要版本中引入破坏性更新。比如，2.3.0版本不一定能和2.2.1版本兼容。另一个栗子时2.1.6版本修改了通知的权限节点格式。

语义化版本控制的目的是通过明确地定义兼容性来避免误会和依赖相冲突。AdvancedBan不遵守语义化版本控制的现状，意味着依赖其前一版本的代码可能会在看似细微的更新之后无法正常运行。

参考资料：
  * https://www.spigotmc.org/resources/advancedban.8695/update?update=367825
  * https://www.spigotmc.org/resources/advancedban.8695/update?update=321631

### 价格

AdvancedBan和LibertyBans均免费发布。

过去AdvancedBan曾经有一次更新，**但是作者对升级数据格式的服务进行收费。**该更新修改了插件的数据格式，但是AdvancedBan并没有为旧版本用户提供一个免费的迁移工具。<sup id="note9ret">[9](#note9)</sup>

### 用户理念

AdvancedBan更倾向于满足用户的各种需求，而LibertyBans则更注重*正确地实现*而非追求便捷。走捷径往往会引发问题。

一个案例就是AdvancedBan当中一个接管其他插件命令的漏洞。比如，如果您同时使用了Essentials插件，`/essentials:ban`可能实际上会变成AdvancedBan的命令！这是个很诡异的行为。它会使得产生这样的奇怪报错：

> Unhandled exception during tab completion for command '/tempban Siikeee ' in plugin Essentials v2.18.1.0

尽管实际上是AdvancedBan在管理/tempban命令。<sup id="note10ret">[10](#note10)</sup>另请参阅[完整的错误日志](https://pastebin.com/v46X9J2M)。

LibertyBans更相信用户的管理能力，因此不会接管其他插件的命令。如果出现了命令冲突，用户自己应当通过恰当的方式解决冲突（如使用服务器的commands.yml，定义命令别名，等等）。

## 依赖要求

总体而言，LibertyBans使用更现代的技术，但是也会有更高的要求。

### Java版本支持

LibertyBans需要Java 17，而AdvancedBan仅需Java 8。

### 外部数据库

两个插件中都可选地使用外部数据库。AdvancedBan和LibertyBans都会默认使用HSQLDB。

LibertyBans对于数据库服务端有特定的最低版本要求。它至少需要MySQL 8.0，MariaDB 10.6，或者PostgreSQL 12。旧版本不受支持。

### 平台支持

LibertyBans支持Sponge和Velocity，而AdvancedBan不支持。二者都支持Bukkit和Bungeecord。

注意AdvancedBan有对Velocity“不稳定”的支持，但是这个功能有已知的漏洞。贡献者说这项支持现已不再维护。<sup id="note11ret">[11](#note11)</sup>

## 功能

AdvancedBan和LibertyBans都有一些另一方不包含的功能。

### 核心处罚类型

* LibertyBans和AdvancedBan均支持封禁、IP封禁、禁言、警告、踢出处罚。
* AdvancedBan支持对玩家进行标注，而LibertyBans不支持。
* 得益于LibertyBans的灵活设计，它也支持对IP地址进行禁言、警告或踢出（虽说很少被使用）。添加这些功能没有任何额外开销。

### 对抗处罚逃避（应对小号）

LibertyBans支持多种基于IP的处罚严格等级来自动处理小号。它也同样具有检查小号的命令，同时能够在已封禁玩家的小号进入服务器时自动发送通知。

AdvancedBan没有类似的功能。不过它确实有一个查询玩家IP地址的命令。

### 豁免

尽管两个插件都支持阻止管理员互相封禁的豁免功能，它们各自的实现有细微的差异。

对于离线玩家，AdvancedBan的权限检查依赖于LuckPerms（代理端）或Vault权限节点（单服务器）。如果这些需求未被满足，离线玩家的权限检测会静默地被跳过。

LibertyBans的豁免功能不会被静默地跳过。不过，这就会导致更多的依赖项。LibertyBans要求安装支持的豁免提供者——目前是LuckPerms或者Vault。如果提供者不存在，该功能完全不可用。

### 从其他插件导入

LibertyBans支持导入AdvancedBan、BanManager、LiteBan、和原版服务器导入数据。

AdvancedBan没有提供任何导入功能。

### 切换数据存储

LibertyBans通过自我导入功能允许用户切换数据存储方式。

AdvancedBan本身并没有提供这项功能，但是有[一个附属插件](https://github.com/A248/AdvancedBanMigrator)实现了这一功能。

### 处罚记录列表

AdvancedBan有/banlist命令，但实际上是一个包含所有生效处罚记录的完整列表。

LibertyBans的/banlist只显示封禁记录，同时/mutelist只显示禁言记录。

二者都提供/history和/warns命令，其功能也相似。

LibertyBans还提供了AdvancedBan中不存在的/blame命令。

### 多代理/多实例同步

LibertyBans在启用功能的时候会在多个实例之间同步处罚记录。这一般用于多个代理的配置。

### 预设

AdvancedBan有一个名为预设（Layouts）的功能，会为管理员提供预定义的处罚原因。

## 参考资料

<a id="note1">1</a>: A248. "Singleton initialization beginning to show its weakness." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/406 [↩](#note1ret)

<a id="note2">2</a>: A248. Discord message in Leoko.dev Discord guild. https://discord.com/channels/339110599084736522/380068075610963968/923016591585726554 [↩](#note2ret)

<a id="note3">3</a>: jeeukko. "Store UUID of punisher in the database." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/435 [↩](#note3ret)

<a id="note4">4</a>: A248. "Race condition between adding new bans and checking if the user is already banned." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/541 [↩](#note4ret)

<a id="note5">5</a>: A248. "Database Queries on the Main Thread." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/458 [↩](#note5ret)

<a id="note6">6</a>: AerWyn81. "ConcurrentModificationException on tempban command executed by another plugin." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/462 [↩](#note6ret)

<a id="note7">7</a>: Leoko. Discord message in Leoko.dev Discord guild. https://discord.com/channels/339110599084736522/339110599084736522/745057107769819267 [↩](#note7ret)

<a id="note8">8</a>: A248. "Database queries need not block each other." AdvancedBan Pull Request. https://github.com/DevLeoko/AdvancedBan/pull/393 [↩](#note8ret)

<a id="note9">9</a>: Leoko. "AdvancedBan [1.7-1.12] v2.1.0 | Performance update." SpigotMC Resource Update. https://www.spigotmc.org/resources/advancedban.8695/update?update=180773 [↩](#note9ret)

<a id="note10">10</a>: A248. "Friendly Register Commands Config Option. AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/pull/373 [↩](#note10ret)

<a id="note11">11</a>: Hopefuls. Comment on "Create AdvancedBan on velocity." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/547#issuecomment-1013552009 [↩](#note11ret)

### 免责声明

请注意，本文没有任何贬损其中插件主体的目的。如有冒犯，我们深表歉意；请联系我们，我们会修改其措辞。
