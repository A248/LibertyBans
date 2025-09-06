
本页面是一份在代理端和群组服安装LibertyBans的一站式指南。如果您没有运行Velocity或BungeeCord，那么这不是您需要了解的东西。

### 基本信息

作为一个群组服的管理者，您有许多种方法安装这类代理端软件。像LibertyBans这样的插件可以直接在代理端运行，或者在后端服务器上运行，甚至同时在两端一起运行（但是不建议）。本页面将描述每一种安装方法的优势与劣势、及对应的需求和步骤，以确保各项功能正常运作。

### 在代理端安装

在单一代理端安装LibertyBans是最简单的方法，并且能适应大多数用户。

操作步骤：
1. 安装一个代理端权限管理插件。
2. 在代理端安装LibertyBans插件。
3. 在代理端配置权限节点。
4. 如果您运行1.19.4及以上版本的Velocity，您需要在代理端**和**后端服务器安装[SignedVelocity](https://modrinth.com/plugin/signedvelocity)插件。

注意，代理端和后端服务器的权限是分开的。有些权限管理插件会在代理端和后端服务器使用不同的命令：比如LuckPerms在BungeeCord上运行时使用`/lpb`命令，而非`/lp`命令。最后，授予OP权限没用，因为代理端不存在OP权限。

### 在后端服务器安装

有些人可能想要在多个后端服务器安装LibertyBans。以下是一些原因：
* 后端服务器无法运行代理端的控制台命令。所以如果您想要让后端的插件（比如反作弊插件）执行处罚命令，您需要在后端服务器安装LibertyBans。
* 后端服务器的插件无法使用代理端插件的API。所以您如果在后端服务器安装了依赖LibertyBans的插件（比如Discord Webhook支持，或者处罚GUI界面），您需要在后端服务器安装LibertyBans。
* 您不愿意安装SignedVelocity，LuckPerms-Velocity或者LuckPerms-BungeeCord。您只想要一个简单的后端插件，不想要代理端插件，因为它们更新起来很麻烦，需要重启整个代理端。

如果是这些情况，请在每一个后端服务器安装LibertyBans。您可以在每个服务器之间复制配置文件。不过，最重要的事情是**进行多实例同步的配置**。

请继续阅读，了解操作方法。

### 覆盖部分后端服务器

一些用户可能想要在一部分后端服务器安装LibertyBans。比如说，您可能需要一个被封禁用户能够加入的"hub"服务器，但是不允许他们加入PvP或生存服务器。或者您在运行一个带有验证服务器的盗版群组服。

如果这符合您的情况，请**关闭**config.yml中的`platforms.game-servers.use-plugin-messaging`选项。关闭此功能之后，被封禁的玩家不会从整个群组服被踢出，而是会根据代理端的配置被送到大厅服务器。

### 盗版群组服

如果您正在运行一个盗版群组服——即允许未验证的Minecraft账户加入的群组服——要小心，LibertyBans只能"看到"经过完整验证的玩家。

默认情况下，LibertyBans会记录每一个登陆玩家的UUID和IP地址。如果LibertyBans装在了代理端，并且未经验证的玩家加入了群组服，这将会产生问题，甚至导致误封IP。

为避免这种情况，您应当设立一个Limbo服务器。Limbo服务器是一个特殊的后端服务器，所有未经验证的玩家都应该被送去那里，并且在通过验证之前不能离开这个服务器。

在配置好Limbo服务器后，请在config.yml中找到`alts-registry`。在那里可以防止加入Limbo服务器的未验证玩家的IP被LibertyBans记录下来。以下是操作步骤：
* 如果您在代理端上运行LibertyBans：
  * 将`register-on-connection`设为false。
  * 在`servers-without-ip-registration`内写入Limbo服务器的名称。
* 如果您在后端服务器运行LibertyBans：
  * 在Limbo服务器的LibertyBans配置中，将`register-on-connection`设为false。
  * 在其他所有服务器配置中将`register-on-connection`设为true。

此外，您也可以在部分后端服务器安装LibertyBans来实现相同的效果：
  * 只在通过验证的玩家能加入的服务器安装LibertyBans。也就是说，您应该在除了Limbo服务器的所有服务器安装插件。
  * 这样也能实现排除错误IP记录的目的，同时您也不需要调整`alts-registry`的配置。
  * 注意被封禁的玩家将可以加入Limbo服务器，但这对于您来讲应该不是个问题。

### 多重代理端

如果您来到了这里，您应该知道您在做什么。请告诉我们LibertyBans如何适应您的需求，哪里可以改进，以及您是否发现任何性能瓶颈。

## 多重实例

### 目的与需求

我们考虑到两个您可能需要运行多个LibertyBans实例的原因：
* 您在运行多个代理端
* 您在运行单个代理端，但是需要在所有后端服务器安装LibertyBans

您需要准备好：
* 一个外部数据库，如MySQL、MariaDB或PostgreSQL。

### 配置数据库

请在所有的实例中将`sql.yml`配置为连接到同一个数据库。

使用默认的本地数据库HSQLDB同步处罚信息是不可能的。所以请在每一个实例——每一个LibertyBans安装的地方——进行数据库配置。

您也需要在`sql.yml`激活多实例同步的功能。请把`synchronization.mode`设置为"ANSI_SQL"，这样LibertyBans能够利用数据库传递信息。
```yaml
synchronization:
  mode: 'ANSI_SQL' # 在服务器之间同步处罚信息
```

如果未来有需求，我们可能会实现更多的同步模式（如Redis，RabbitMQ，Kafka等）。

### 最后的注意事项和限制

**踢出离线玩家**：如果您使用多实例同步，您将可以"踢出"离线玩家，这也会被记录到处罚历史之中。

**同时在代理端和后端服务器安装**：特别不建议在两端同时安装LibertyBans，我们也不会为此提供官方支持。不过我们会尝试在您遇到问题时提供一些帮助。您需要调整代理端和后端服务器的权限，避免在执行新处罚时收到多个通知消息。
