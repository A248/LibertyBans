
LibertyBans以扩展程序的形式提供了一些额外功能。

安装扩展不会造成额外的性能开销：它们会直接注入LibertyBans的核心，而无需经过任何中间层。以扩展的形式实现功能是保持插件可持续开发的一种设计考量。

## 安装扩展

你可以简单地使用 `/libertybans addon install <扩展名>` 来安装扩展。请把 `<扩展名>` 替换成你想要使用的扩展的ID。命令执行时，会把所需的扩展释放到文件系统中，这样下次重启的时候就可以加载了。

你也可以下载扩展jar文件，并把它放到 `plugins/LibertyBans/addons`目录中，最后重启服务器来安装扩展。

已安装扩展的数量会在启动时显示出来：
```
[LibertyBans] Detected 2 addon(s)
```

## 配置、管理扩展

扩展的配置文件和扩展jar文件的位置相同——它们都在LibertyBans插件文件夹的`addons`目录中。

在配置完扩展之后，请运行`/libertybans reload`。

# 可用的扩展

每个扩展的ID都在[中括号]中呈现。

## 添加新命令

### 检查处罚记录 [command-checkpunish]

此扩展启用了`/libertybans checkpunish <id>`命令。该命令可以根据提供的处罚ID显示一次处罚操作的具体信息。

命令需要权限`libertybans.addon.checkpunish.use`。

### 检查用户 [command-checkuser]

此扩展提供了`/libertybans checkuser <玩家>`命令。该命令会显示指定玩家被封禁或禁言的情况。

命令需要权限`libertybans.addon.checkuser.use`。

### 清除 [command-expunge]

此扩展提供了`/libertybans expunge <id> command`命令。该命令可以从数据库中移除指定的处罚记录。适用于清理无效或意外的处罚。

**警告：被清理的记录不可恢复。**

命令需要权限`libertybans.addon.expunge.use`。

### 延期 [command-extend]

此扩展提供了`/libertybans extend <id> <时间>`命令。您可以指定一个现有且未过期的处罚，将其额外延长一定时间。

命令需要权限`libertybans.addon.extend.use`。

### 管理操作回滚 [command-staffrollback]

此扩展提供了`/libertybans staffrollback <管理员> [时间]`命令。执行回滚操作会彻底清除特定管理人员执行的全部处罚。适用于管理恶意处罚或者账号遭窃的情况。

`[时间]`参数确定了多久之前的处罚操作应该被清除。早于该时间段的处罚操作仍会被保留。

确定了多久之前的惩罚应该被清除。**警告：被清理的记录不可恢复。**

命令需要权限`libertybans.addon.staffrollback.use`。

## 豁免

豁免功能允许你保护部分玩家不被处罚。比如，你可能想要阻止协管封禁正式管理员，并且你绝不会想要让服主被封禁。

LibertyBans允许你做到这一点。你可以配置一些豁免规则，使得服主能封禁管理员、管理员能封禁协管，但是协管不能封禁管理员。

由于不存在统一的权限API，为了让该功能正常运作，LibertyBans需要你选择并安装一个支持程序：

1. LuckPerms的用户组权重
2. Vault权限

注意：
* 该功能阻止低级管理员处罚高级管理员的拒绝消息可以在messages.yml文件中配置。
* 针对IP地址的处罚不考虑豁免机制。豁免功能适合与复合处罚结合对玩家使用。
* 在处罚用户后为该用户赋予豁免权，并不能自动撤销现有的处罚。

### 基于LuckPerms用户组权重的豁免 [exemption-luckperms]

此扩展只能和LuckPerms共同使用，因为它利用了LuckPerms的用户组权重功能。

配置很简单。只需确保为用户组设置权重。高级的管理员用户组应当具有更高的权重。

管理员可以处罚用户组权重比他自己低的玩家。注意LibertyBans会考虑每名玩家的所有权限组，来确定该用户的最高用户组权重。

### 基于Vault权限的豁免 [exemption-vault]

此扩展根据带等级的权限提供豁免功能。这需要一个与Vault兼容的权限插件，且只支持Spigot或Paper服务器。

扩展通过定义豁免等级来运作。豁免等级越高，管理员权限也越高。豁免等级低的管理员无法处罚豁免等级高的管理员。

配置：
1. 授予权限`libertybans.<类型>.exempt.level.<豁免等级>`。
2. 将`max-level-to-scan-for`配置设置为你授予过的最高豁免等级。
3. 调整`permission-check-thread-context`配置的值来适配你的权限管理插件。

## 预设 [layouts]

预设扩展提供了`/libertybans punish`命令。它可以用直观而强大的方式来定义处罚模板、处罚阶梯、处罚路径……反正就是类似的东西。

使用预设路径执行的处罚和其他的处罚方法类似。当然了，使用相同路径执行的新处罚会使用配置的处罚阶梯来计算新的处罚细节。

### 配置

配置的主要机制是为每一个预设路径定义一个阶梯。如果玩家受到了大于等于指定的处罚次数，插件就会按照阶梯上的内容来决定具体实行的处罚。

### 权限

使用预设扩展的权限节点与执行常规处罚的类似。不同的是，预设插件通过预设路径来进行区分。设置权限时，请将`<路径>`替换成你配置中的预设路径名。

* `libertybans.addon.layout.use.<路径>.target.uuid` - 处罚玩家
* `libertybans.addon.layout.use.<路径>.target.ip` - 处罚IP地址
* `libertybans.addon.layout.use.<路径>.target.both` - 一次性同时处罚玩家和IP地址
* `libertybans.addon.layout.use.<路径>.silent` - 使用静默功能

注意，接收处罚通知的权限仍然是与处罚类型绑定的。换句话来说，`libertybans.<类型>.<do|undo>.<notify|notifysilent>`这个权限节点依旧生效。没有接收特定预设的处罚通知的权限节点。

### 与豁免扩展的联动

如果你在使用基于Vault权限的豁免扩展，那么可以用`libertybans.layout.exempt.level.<豁免等级>`这个权限节点来定义豁免等级。（由于检查豁免等级发生在执行处罚前，原有的权限节点无法使用。）

## 其他

### 原因快捷方式 [shortcut-reasons]

该扩展允许为常用的处罚原因设置快捷方式。比如，`/ban A248 30d #hacking`可以和`/ban A248 30d 你因为开挂被封禁了！`等效。

扩展配置也很直观。如果管理员使用了无效的快捷方式，命令不会被执行，防止出现意外错误。快捷方式区分大小写。

### 警告操作 [warn-actions]

允许在达到特定警告次数后执行操作，例如执行命令或者施加附加的处罚。

此扩展相对来讲易于使用和配置。

### Webhook [webhook]

允许您向一个HTTP节点发送一个含有处罚信息和事件的webhook。

大多数情况下，该扩展是用于Discord Webhook的。比如说，您可以在有人被封禁时向您的Discord服务器发送公告。
