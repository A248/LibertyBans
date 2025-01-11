
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

* 如果启用了持续时间权限：* 需要使用 `libertybans.ban.dur.<timespan>` 来控制玩家封禁时长。
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

### Scopes

If scope permissions are enabled, additional permissions are required to punish and list punishments. See the [Scoped Punishments](Scoped-Punishments.md) page.

## 附加功能

附加功能的具体权限在 [附加组件](Addons) 页面上有说明。

# LibertyBans 0.8.x

在 0.8.x 中，封禁、禁言、警告和踢除的权限都遵循此模板：

* `libertybans.ban.command` - required as a prerequisite for banning
* `libertybans.ban.ip` - required for banning _IPs_
* `libertybans.ban.undo` - required as a prerequisite for unbanning
* `libertybans.ban.undoip` - required for unbanning _IPs_
* `libertybans.ban.silent` - use the silent feature for bans (e.g. `ban -s`)
* `libertybans.ban.silentundo` - use the silent feature for unbans (e.g. `unban -s`)
* `libertybans.ban.notify` - receive notifications for bans
* `libertybans.ban.unnotify` - receive notifications for unbans
* `libertybans.ban.notifysilent` - receive notifications for all bans, even those executed with "-s"
* `libertybans.ban.unnotifysilent` - receive notifications for all unbans, even those executed with "-s"

Replace `ban` with `mute`, `warn`, or `kick` for permissions relevant to other punishment types.

Duration permissions are the same as in 1.0.0.

另请参阅 [从 0.8.x 升级至 1.0.0](Upgrading-to-LibertyBans-1.0.0-from-0.8.x)
