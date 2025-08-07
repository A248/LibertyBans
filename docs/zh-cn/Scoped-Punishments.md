
# Scoped Punishments

The server scopes feature allows you to ban players from specific backend servers on a network such as BungeeCord or Velocity.

For example, perhaps you want to ban a player from the PvP server for combat hacking; however, they beg and plead until eventually convincing you to allow them to play the Creative game mode on a different backend server.

Scope configuration is controlled by the `scope.yml`.

## Introduction

There are three kinds of scopes:
1. The global scope, applying everywhere.
2. Per-server scopes.
3. Category scopes, applying to a group of servers.

### Server Name

The server name controls how the server itself is identified in per-server scopes. This is the same server name you configured proxy-wide: on BungeeCord, in the `config.yml`, and for Velocity, the `velocity.toml`.

Usually, backend servers can automatically detect their server name (using a technique called plugin messaging). However, if automatic detection is insufficient, you can override the detected server name.

On the proxy, LibertyBans will always know each backend server's name. The proxy *itself* can also be named by setting a value manually, although per-proxy punishments have limited practical use.

### Categories

Categories may be configured per-server. Simply write out the categories it will be included in.

## Commands

Scopes may be used in commands by specifying `-server=<server>` with a server name, `-category=<category>` with a category, or `-scope=<scope>` with *any* scope.

With `-scope=<scope>`, the scope itself must be one of `server:<server>`, `category:<category>` or `*` for the global scope. For example, `-scope=category:pvp` is valid.

### Punish commands

If no scope is specified, punishing commands will use the configured default punishing scope. You may change this to the current server to ban players on the same server by default.

### Listing commands

Listing commands such as /banlist, /history, etc. will include punishments from all scopes. Specifying a scope will narrow the list of punishments to ones using that scope -- critically, this is NOT the same as punishments *applying* to that scope. For example, `/banlist -scope=*` will yield only punishments with the global scope, but will not show punishments with per-server or category scopes.

### Examples

* /ban A248 -server=lobby You're just too annoying in the lobby
* /mute A248 -category=clans A conversationalist is far too dangerous of a clans player
* /warn A248 -scope=* 7d It's possible to place the -server, -category, or -scope argument anywhere before the punishment reason
* /kick A248 Did you know that scoped kicks work too? The player will only be kicked if they're currently connected to the given scope

### Permissions

By default, permissions for scopes are disabled for compatibility. To require specific permissions, turn on `require-scope-permissions`.

If enabled:
* Staff must have the `libertybans.scope.global`, `libertybans.scope.server.<server>`, or `libertybans.scope.category.<category>` permissions.
* The permission `libertybans.scope.default` must be granted to use commands without a scope argument. Not granting this permission requires staff members to specify an explicit scope.
