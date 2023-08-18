This page starts with LibertyBans 1.0.x. For LibertyBans 0.8.x, please scroll down.

# LibertyBans 1.0

### Any Command ###

As a pre-requisite for any command, you must have the permission:

`libertybans.commands`

## Punishment ##

### Bans ###

* `libertybans.ban.do.target.uuid` - ban players
* `libertybans.ban.do.target.ip` - ban IP addresses
* `libertybans.ban.do.target.both` - ban player and IP address in the same punishment
* `libertybans.ban.do.silent` - use the silent feature for bans (e.g. `ban -s`)
* `libertybans.ban.do.notify` - receive notifications for bans
* `libertybans.ban.do.notifysilent` - receive notifications for bans executed with "-s"
* `libertybans.ban.undo.target.uuid` - unban players
* `libertybans.ban.undo.target.ip` - unban IP addresses
* `libertybans.ban.undo.target.both` - unban a player and IP address from the same punishment
* `libertybans.ban.undo.silent` - use the silent feature for unbans (e.g. `unban -s`)
* `libertybans.ban.undo.notify` - receive notifications for unbans
* `libertybans.ban.undo.notifysilent` - receive notifications for unbans executed with "-s"

* *If duration permissions are enabled:* `libertybans.ban.dur.<timespan>` is needed to ban players for an amount of time.
  * Use _libertybans.ban.dur.perm_ for permanent duration.
  * The timespan follows the same format as for commands. `libertybans.ban.dur.6d` grants permission to ban for six days at most.
  * If a player has multiple duration permissions, the longest duration is used.

### Mutes ###

Mute permissions are similar to ban permissions, except that they start with `libertybans.mute` instead.

### Warn ###

Warn permissions are similar to ban permissions, except that they start with `libertybans.warn` instead.

### Kicks ###

Kick permissions are similar to ban permissions, except that they start with `libertybans.kick` instead.

For obvious reasons, there are no duration permissions for kicks; moreover there is no "undo" for kicks.

## Lists

* `libertybans.list.banlist` - /banlist
* `libertybans.list.mutelist` - /mutelist
* `libertybans.list.history` - /history
* `libertybans.list.warns` - /warns
* `libertybans.list.blame` - /blame

## Alt Management

* `libertybans.alts.command` - /alts command
* `libertybans.alts.autoshow` - see a report when a player joins on a detected alt account
* `libertybans.alts.accounthistory.list` - /accounthistory list
* `libertybans.alts.accounthistory.delete` - /accounthistory delete

## Administration

* `libertybans.admin.debug` - /libertybans debug
* `libertybans.admin.reload` - /libertybans reload
* `libertybans.admin.restart` - /libertybans restart
* `libertybans.admin.addon` - /libertybans addon
* `libertybans.admin.import` - /libertybans import
* `libertybans.admin.viewips` - Allows staff to view IP addresses if *censor-ip-addresses* is turned on in the configuration.

### Scopes

If scope permissions are enabled, additional permissions are required to punish and list punishments. See the [Scoped Punishments](Scoped-Punishments.md) page.

## Addons

Addon-specific permissions are described on the [Addons](Addons) page.

# LibertyBans 0.8.x

In 0.8.x, the permissions for bans, mutes, warns, and kicks follow this scheme:

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

See also [the migration guide to update to LibertyBans 1.0.0](Upgrading-to-LibertyBans-1.0.0-from-0.8.x)
