
LibertyBans provides several additional features in the form of addons.

There is no performance cost or other overhead from installing addons -- they hook directly into the main LibertyBans core with no intermediaries. Placing a feature into an addon is a design decision to keep the plugin maintainable.

## Installing Addons

1. Download the addon jar: [All addons may be downloaded here](https://ci.hahota.net/job/LibertyBans/).
2. Place the addon jar in the `addons` directory of the LibertyBans plugin folder.
3. Restart the server.

The number of installed addons is displayed at startup:
```
[LibertyBans] Detected 2 addon(s)
```

## Configuring and Managing Addons

Addon configuration is located in the same place as the installed addon jars -- the `addons` directory of the LibertyBans plugin folder.

After configuring an addon, run `/libertybans reload`.

Related commands:
* `/libertybans addon list` - Lists installed addons.
* `/libertybans addon reload <addon identifier` - Reloads the configuration of a single addon only.

# Available Addons

## New Commands

### CheckPunish

This addon provides the `/libertybans checkpunish <ID>` command. This command displays the details of a specific punishment given the punishment ID.

Requires the permission `libertybans.addon.checkpunish.use`.

### CheckUser

Provides the `/libertybans checkuser <player>` command. This command displays if the given player is banned or muted.

Requires the permission `libertybans.addon.checkuser.use`.

### StaffRollback

Provides the `/libertybans staffrollback <operator> [time]` command. Performing a rollback fully purges all punishments by a certain staff member, which is useful if a staff member becomes rogue or has their account hacked.

The `[time]` argument identifies how long ago punishments should be purged. Punishments made earlier than the given amount of time ago are kept.

**WARNING: Purged punishments are NOT recoverable.**

Requires the permission `libertybans.addon.staffrollback.use`.

## Exemption

Exemption functionality allows you to protect certain players from punishment. For example, you may want to prevent moderators from banning administrators, and you never want the owner to be banned.

LibertyBans allows you to achieve this. You can configure exemption so that owners can ban admins, and admins can ban moderators, but moderators cannot ban admins.

Because there is no unified permissions API, LibertyBans requires you to choose and install an exemption backend for this feature to operate:

1. LuckPerms group weights
2. Vault permissions

Notes:
* The denial message when a staff member tries to punish a superior officer is configurable in the messages.yml file.
* IP-based punishments do not consider exemption. The exemption feature works best with user and composite punishments.
* Granting exemption to a user after the fact will not automatically undo existing punishments.

### Exemption (LuckPerms group weights)

This addon works only with LuckPerms, since it leverages LuckPerms' group weights feature.

Configuration is simple. Make sure to configure LuckPerms' group weights. Higher staff ranks should have bigger weights.

An operator can punish a player if the target player has a lower group weight than the operator. Note that LibertyBans considers every group for each user to determine the user's highest group weight.

### Exemption (Vault permissions)

This addon provides exemption functionality using tiered permissions. It requires a Vault-compatible permissions plugin and only supports Spigot/Paper servers.

The addon operates by defining exemption levels. The higher the exemption level, the more privileged the staff member. Staff members with a lower exemption level cannot punish staff members with a higher level.

Configuration:
1. Grant the permission `libertybans.<type>.exempt.level.<level>` . 
2. Set the `max-level-to-scan-for` to the value of the highest exemption level you granted.
3. Change the `permission-check-thread-context` value to suit your permissions plugin.

## Other

### Shortcut Reasons

Allows substituting faster shortcuts for commonly-used punishment reasons. For example, `/ban A248 30d #hacking` becomes `/ban A248 30d You are banned for hacking`.

Configuration is straight-forward otherwise. If a staff member specifies an invalid shortcut, the command is denied to prevent mistakes. Shortcuts are case-sensitive.

### Warn Actions

Allows executing actions, such as executing commands or inflicting additional punishments, when a certain amount of warns is reached.

This addon is relatively easy to use and configure.
