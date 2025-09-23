
推荐使用 LibertyBans 1.0.x 版本。如需查看关于 LibertyBans 0.8.x 的权限配置，请向下滚动。

# LibertyBans 1.0

### 任何命令 ###

作为执行任何命令的前提条件，您必须拥有权限：

`libertybans.commands`

## 惩罚 ##

### 封禁 ###

* `libertybans.ban.do.target.uuid` - 封禁玩家
* `libertybans.ban.do.target.ip` - 封禁 IP 地址
* `libertybans.ban.do.target.both` - 在同一惩罚中封禁玩家和 IP 地址
* `libertybans.ban.do.silent` - 使用悄悄封禁功能 (也就是 `ban -s`)
* `libertybans.ban.do.notify` - 收到玩家被封禁的通知
* `libertybans.ban.do.notifysilent` - 收到玩家被封禁的通知，即使是悄悄封禁
* `libertybans.ban.undo.target.uuid` - 解封玩家
* `libertybans.ban.undo.target.ip` - 解封 IP 地址
* `libertybans.ban.undo.target.both` - 在同一惩罚中解封玩家和 IP 地址
* `libertybans.ban.undo.silent` - 使用悄悄解封功能 (也就是 `unban -s`)
* `libertybans.ban.undo.notify` - 收到玩家被解封的通知
* `libertybans.ban.undo.notifysilent` -收到玩家被解封的通知，即使是悄悄解封

* 如果启用了持续时间权限：
  * 需要使用 `libertybans.ban.dur.<timespan>` 来控制玩家封禁时长。
  * 使用 _libertybans.ban.dur.perm_ 来获得永久期限。
  * 时间跨度的格式与命令相同。libertybans.ban.dur.6d "允许禁言最多六天。
  * 如果一个玩家有多个持续时间的权限节点，则使用最长的持续时间。

### 禁言 ###

和封禁的权限类似，只是权限以 `libertybans.mute` 开头.

### 警告 ###

和封禁的权限类似，只是权限以 `libertybans.warn` 开头.

### 踢除 ###

和封禁的权限类似，只是权限以 `libertybans.kick` 开头.

由于显而易见的原因，踢除没有持续时间权限；此外，除也没有 “解禁”功能。

## 列表

* `libertybans.list.banlist` - /banlist
* `libertybans.list.mutelist` - /mutelist
* `libertybans.list.history` - /history
* `libertybans.list.warns` - /warns
* `libertybans.list.blame` - /blame

## 小号管理

* `libertybans.alts.command` - /alts 指令
* `libertybans.alts.autoshow` - 当检测到玩家试图开小号登录时收到通知
* `libertybans.alts.accounthistory.list` - /accounthistory 列表
* `libertybans.alts.accounthistory.delete` - /accounthistory delete 列表

## 管理员

* `libertybans.admin.debug` - /libertybans debug
* `libertybans.admin.reload` - /libertybans reload
* `libertybans.admin.restart` - /libertybans restart
* `libertybans.admin.addon` - /libertybans addon
* `libertybans.admin.import` - /libertybans import
* `libertybans.admin.viewips` - 如果在配置中开启了*censor-ip-addresses*，则允许所有管理员查看玩家的 IP 地址。

### 范围功能

如果启用了范围权限，还需要设置额外的权限来使用处罚和列表命令。请参阅[范围处罚](zh-cn/Scoped-Punishments.md)页面。

## 附加功能

附加功能的具体权限在 [附加组件](zh-cn/Addons) 页面上有说明。

# LibertyBans 0.8.x

在 0.8.x 中，封禁、禁言、警告和踢除的权限都遵循此模板：

* `libertybans.ban.command` - 使用封禁功能的基础权限
* `libertybans.ban.ip` - 用于封禁_IP_
* `libertybans.ban.undo` - 使用解封功能的基础权限
* `libertybans.ban.undoip` - 用于解封_IP_
* `libertybans.ban.silent` - 用于悄悄封禁 (如`ban -s`)
* `libertybans.ban.silentundo` - 用于悄悄解封 (如 `unban -s`)
* `libertybans.ban.notify` - 接收封禁通知
* `libertybans.ban.unnotify` - 接收解封通知
* `libertybans.ban.notifysilent` - 接收所有封禁通知，包括使用 "-s" 的悄悄封禁
* `libertybans.ban.unnotifysilent` - 接收所有解封通知，包括使用 "-s" 的悄悄解封

将`ban`替换为`mute`、`warn`、或`kick`即可授予其他类型处罚相关的权限。

限制时长的权限与1.0.0版本中相同。

另请参阅 [从 0.8.x 升级至 1.0.0](zh-cn/Upgrading-to-LibertyBans-1.0.0-from-0.8.x)
