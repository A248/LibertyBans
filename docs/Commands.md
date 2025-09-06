
This page describes how to change and manage command aliases.

## Basics

### Layout

Every command can be run with `/libertybans <cmd>`.

For example, writing `/ban` is equivalent to `/libertybans ban` under the default installation.

### Changing built-in aliases

By default, LibertyBans registers aliases for commonly-used commands, as defined in the config.yml:

```yaml
# ...
commands:
  aliases:
    - "ban" # /ban -> /libertybans ban
    - "ipban" # /ipban -> /libertybans ipban
    # etc.
```

You can remove any aliases which you do not want, or add new ones. This system works with [addon commands](Addons.md), too.

## Deconflicting and managing commands, per platform

Some plugins (like Essentials, CMI) have hardcoded /ban and /kick commands that overlap with LibertyBans and create conflicts. The server doesn't automatically select LibertyBans commands when there is a conflict.

### Bukkit commands.yml

Every Bukkit server has a built-in file called the `commands.yml` which lets you control command aliases. This file is very powerful and can help deconflict banning commands.

Open the commands.yml and modify the `aliases` section:

```yml
aliases:
  ban:
  - libertybans ban $1-
  alts:
  - libertybans alts $1-
  history:
  - libertybans history $1-
```

Repeat this pattern for all the commands you wish to overwrite. This file will take precedence, and it is supposed to override Essentials and vanilla.

### Bukkit alias plugins

Alternatively, Bukkit / Spigot / Paper is an incredibly old platform, and many plugins exist that let you define custom aliases. An example is [MyCommand](https://dev.bukkit.org/projects/mycommand) and others also exist.

### BungeeCord alias plugins

On BungeeCord you can like find a plugin to create command aliases.
We know of multiple, like [TCAlias](https://www.spigotmc.org/resources/t2c-alias-alias-plugin-for-spigot-bungee-commands-1-8-x-1-21.96389/) and [BungeeCommands](https://www.spigotmc.org/resources/bungeecommands-custom-commands-aliases.20771/).

### Velocity alias plugins

[CustomCommands](https://modrinth.com/plugin/customcommandsvelocity) is a Velocity plugin that lets you define your own commands.
 Also, the plugin [Aliasr](https://github.com/tobi406/aliasr) exists, but it was made for Velocity 1.x and just needs to be trivially updated to Velocity 3.x

### Sponge

Sponge provides very little API for managing commands, and commands cannot be unregistered. As a result, LibertyBans disables the alias system when running on Sponge.

Thus, you need to write a custom plugin (with hardcoded commands) if you want aliases.

### Extra Notes

* Some plugins can have their commands disabled: for example, Essentials provides a `disabled-commands` setting. However, these options are sometimes half-baked and don't actually unregister the command.
* If commands are not deconflicted, the server can choose which plugin to use. It is entirely arbitrary, therefore, whether LibertyBans' or Essentials' /ban command is chosen if both are installed. This can make commands appear to stop working "randomly," because the server can select a new command randomly (due to various technical factors).
* If you're running a network and you [install LibertyBans on the proxy](Network-Installation.md), then LibertyBans will intercept commands before they reach the backend servers.

