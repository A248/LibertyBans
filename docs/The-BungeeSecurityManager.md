This page only applies to BungeeCord users.

## Waterfall Users
You do not have to worry. This is a BungeeCord-specific bug. It has been patched in Waterfall.

## BungeeCord Users

By default, BungeeCord includes a SecurityManager implementation called the 'BungeeSecurityManager'. This class is a farce. When plugins from create their own threads outside the BungeeCord scheduler, the BungeeSecurityManager prints huge stacktraces to the logs. This is incredibly annoying for BungeeCord users.

The BungeeSecurityManager makes it harder for plugins to run tasks asynchronously. No one wants console spam. Furthermore, since the majority of LibertyBans is platform-independent, and it extensively takes advantage of threading, special-casing BungeeCord would be non-ideal.

### But it's a SecurityManager! I want security!

The SecurityManager does not provide any real security. It has never prevented malicious code or plugins, and it will never do so. If you allow untrusted code execution, no SecurityManager will save you. You are at the whim of whichever attacker gained access to your system.

In reality, the SecurityManager is a tool used by Java applications to strengthen the integrity of executing code by defining access checks for privileged actions. However, BungeeCord has implemented it poorly, causing unnecessary headaches for plugin developers and users.
