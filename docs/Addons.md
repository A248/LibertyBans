
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

## CheckPunish

This addon provides the `/libertybans checkpunish <ID>` command. This command displays the details of a specific punishment given the punishment ID.

Requires the permission `libertybans.addon.checkpunish.use`.

## StaffRollback

Provides the `/libertybans staffrollback <operator> [time]` command. Performing a rollback fully purges all punishments by a certain staff member, which is useful if a staff member becomes rogue or has their account hacked.

The `[time]` argument identifies how long ago punishments should be purged. Punishments made earlier than the given amount of time ago are kept.

**WARNING: Purged punishments are NOT recoverable.**

Requires the permission `libertybans.addon.staffrollback.use`.

## Warn Actions

Allows executing actions, such as executing commands or inflicting additional punishments, when a certain amount of warns is reached.

This addon is relatively easy to use and configure.
