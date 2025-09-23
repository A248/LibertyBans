
本页面介绍如何修改和管理命令别名。

## 基本内容

### 格式

所有命令都可以以`/libertybans <cmd>`的方式运行。

比如说，在默认配置下，使用`/ban`和使用`/libertybans ban`等效。

### 修改内置别名

如config.yml中所示，LibertyBans默认会为常用的命令注册别名：

```yaml
# ...
commands:
  aliases:
    - "ban" # /ban -> /libertybans ban
    - "ipban" # /ipban -> /libertybans ipban
    # 等等
```

您可以移除任何您不需要的命令别名，或者添加新的别名。这套机制对[扩展命令](zh-cn/Addons.md)也适用。

## 针对平台管理命令并解决冲突

有些插件（如Essentials，CMI）会硬编码/ban和/kick命令，这会覆盖LibertyBans的命令并造成冲突。服务器不会在出现冲突的情况下自动使用LibertyBans的命令。

### Bukkit commands.yml

所有Bukkit服务器都内置一个名为`commands.yml`的文件，您可以通过它管理命令别名。这个文件非常强大，可以帮助解决命令的冲突。

请打开commands.yml文件，并修改aliases部分：

```yml
aliases:
  ban:
  - libertybans ban $1-
  alts:
  - libertybans alts $1-
  history:
  - libertybans history $1-
```

请为所有您希望覆盖的命令都按照上面的格式进行配置。该文件具有优先权，理应覆盖Essentials和原版的命令。

### Bukkit 命令别名插件

此外，Bukkit / Spigot / Paper 都是历史很悠久的平台，于是便有一些插件让您自定义命令别名。一个例子便是[MyCommand](https://dev.bukkit.org/projects/mycommand)，当然还有其他类似的插件。

### BungeeCord 命令别名插件

在BungeeCord您也可以找到设置命令别名的插件。   
我们了解一些这类的插件，如[TCAlias](https://www.spigotmc.org/resources/t2c-alias-alias-plugin-for-spigot-bungee-commands-1-8-x-1-21.96389/)和[BungeeCommands](https://www.spigotmc.org/resources/bungeecommands-custom-commands-aliases.20771/).

### Velocity 命令别名插件

[CustomCommands](https://modrinth.com/plugin/customcommandsvelocity) 是一个让您定义自己的命令的插件。   
  此外，还有[Aliasr](https://github.com/tobi406/aliasr)这个插件，但是它是为Velocity 1.x版本制作的，目前需要更新到Velocity 3.x版本。

### Sponge

Sponge并没有提供管理命令的API，并且命令也无法在运行时取消注册。因此，LibertyBans会在Sponge上运行时禁用别名系统。

因此，如果您需要命令别名，您需要自行编写一个硬编码命令的插件来实现。

### 额外注意事项

* 有些插件可以自行关闭它们的命令。比如说，Essentials提供了一个`disabled-commands`设置。但是，这些选项有时候半斤八两，并不能真正地取消注册这些命令。
* 如果命令没有冲突，服务器就可以选择要使用的插件。但这个过程很随意，因此，当LibertyBans和Essentials同时安装时，您无法预测是哪个插件的命令会被执行。这会使得插件的命令看起来是"随机失灵"，因为受各种技术因素的影响，服务器很可能会随机选择一个新的命令。
* 如果您组建了一个服务器网络，并且[将LibertyBans安装在了代理端上](zh-cn/Network-Installation.md)，LibertyBans就会在命令抵达后端服务器之前获取并执行命令。

