
This page describes how to change command aliases. This can be used to rename commands, for example.

## Layout

Every command can be run with `/libertybans <cmd>`.

For example, writing `/ban` is equivalent to `/libertybans ban` under the default installation.

## Disabling Built-In Aliases

By default, LibertyBans registers aliases for commonly-used commands, as defined in the config.yml:

```yaml
# ...
commands:
  aliases:
    - "ban" # /ban -> /libertybans ban
    - "ipban" # /ipban -> /libertybans ipban
    # etc.
```

You can remove any aliases which you do not want.

## Creating New Aliases

LibertyBans is a punishment plugin, not an alias plugin. To define your own aliases, you should use another plugin which is made for the task.

### Bukkit

On Bukkit, you don't need to install another plugin.

Every Bukkit server has a file called the `commands.yml` which allows you to control command aliases. This file is very powerful and will help you customize your server's vast array of commands.

### BungeeCord

On BungeeCord you should find a plugin which allows you to create command aliases.

We know of multiple plugins in existence to do this.

### Velocity

We do not currently know of any command alias plugins for Velocity that are updated for Velocity 3.x

The plugin [Aliasr](https://github.com/tobi406/aliasr) exists, but was made for Velocity 1.x and has not been updated. We suggest that you find a developer to update this plugin to Velocity 3.x -- it wouldn't  be very difficult considering Aliasr is a very small plugin and it would be trivial to update it to Velocity 3.x
