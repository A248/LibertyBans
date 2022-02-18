This is a quick table for comparing between LibertyBans, AdvancedBan, and LiteBans.

**NB: *Tables and graphs do not tell the full picture!*** To really understand what's going on here, read the detailed and explanatory 
comparisons. You can find these on the sidebar at the right. For each plugin here, there is a detailed comparison between LibertyBans and the plugin.

## Table

<!-- Platform logos -->

[Bukkit]:https://media.forgecdn.net/avatars/97/684/636293448268093543.png
[Sponge]:https://www.spongepowered.org/favicon.ico

<!-- License logos -->

[AGPL]:https://www.gnu.org/graphics/agplv3-155x51.png
[GPL]:https://www.gnu.org/graphics/gplv3-127x51.png
[CC-BY-NC]:http://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc.png

| Plugin      | Supported Platforms                                                                                                                                                                                                                                                           | Java Req. | Free (Gratis) | Open License   | Database Support                           | Thread-Safe Design | Stable API | Geyser Support | Multi-Instance Support | Connection Pool | Exempt Permissions | Server Scopes | Uses UUIDs | Schema Integrity | Import From                                                                           |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|---------------|----------------|--------------------------------------------|--------------------|------------|----------------|------------------------|-----------------|--------------------|---------------|------------|------------------|---------------------------------------------------------------------------------------|
| LibertyBans | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee<br/> <img src="https://raw.githubusercontent.com/PaperMC/velocitypowered.com/5878ae0941e3adff3a238fe9020250c5f01c2899/src/assets/img/velocity-blue.png" width=16>Velocity | 11+       | ✔️            | ✔️ ![AGPL]     | HSQLDB (local), MariaDB, MySQL, PostgreSQL | ✔️                 | ✔️         | ✔️             | ✔️                     | ✔️              | ❌                  | ❌             | ✔️         | ✔️               | AdvancedBan<br/>BanManager<br/>LiteBans<br/>vanilla                                   |
| AdvancedBan | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee                                                                                                                                                                           | 8+        | ✔️            | ✔️ ![GPL]      | HSQLDB (local), MariaDB, MySQL             | ❌                  | ❌          | ❓              | ❌                      | ❌               | ✔️                 | ❌             | ➖          | ❌                |                                                                                       |
| BanManager  | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee<br/> <img src="https://www.spongepowered.org/assets/img/icons/spongie-mark.svg" width=16>Sponge                                                                           | 8+        | ✔️            | ➖️ ![CC-BY-NC] | H2 (local), MariaDB, MySQL                 | ❌️                 | ❌          | ➖              | ✔️                     | ✔️              | ❌                  | ➖             | ✔️         | ✔️               | AdvancedBan<br/>vanilla                                                               |
| LiteBans    | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee<br/> <img src="https://raw.githubusercontent.com/PaperMC/velocitypowered.com/5878ae0941e3adff3a238fe9020250c5f01c2899/src/assets/img/velocity-blue.png" width=16>Velocity | 8+        | ❌             | ❌ Proprietary  | H2 (local), MariaDB, MySQL, PostgreSQL     | ❓                  | ➖          | ❌              | ✔️                     | ❓               | ✔️                 | ✔️            | ✔️         | ❌                | AdvancedBan<br/>BanManager<br/>BungeeAdminTools<br/>MaxBans<br/>UltraBans<br/>vanilla |

Legend:

✔️ – Yes

➖ – Partially

❌ – No

❓ - Unknown, either because the plugin is closed-source, or the feature has not been tested.

## Categories

Some categories require further explanation because they are not self-explanatory. They are described here.

Some categories are too complex to describe here. When this is the case, it is necessary to read the detailed plugin-by-plugin comparisons. You can find these on the sidebar at the right. For each plugin here, there is a detailed comparison between LibertyBans and the plugin.

### Supported Platforms

Which platforms are supported by a stable release of the plugin.

### Java Requirement

The minimum Java version needed to run the software.

### License

Whether the software is under a free software license. Requirements:
1. Freedom of source code: the software is open-source
2. Freedom of use: the user can use the software for whatever purpose

BanManager's license prohibits commercial use. This makes it **illegal** to use BanManager for monetary profit. However, it is open-source; hence the "partial" ranking.

Disclaimer: This is not legal advice and the writer is not a lawyer.

### Thread-Safe Design

If a plugin is not thread-safe, it will have unreliable and buggy behavior. However, some of this unreliable behavior may only occur in exceptional and rare circumstances, making it very hard to debug and diagnose.

See the detailed plugin-specific comparisons for more information.

### Stable API

Whether the plugin's API is semantically-versioned and therefore can be relied upon in stable fashion by other plugins.

See [semver.org](https://semver.org/) and the detailed plugin-specific comparisons.

LiteBans' *partial* ranking: LiteBans follows the semver constraint of restricting breaking changes to major versions, but does not follow semver for additions of new API, which *should* be done in minor releases.

### Multi-Instance Support

This feature allows synchronizing punishments across multiple instances of the given plugin. It is relevant for proxies.

This is commonly used for multi-proxy setups, but can also be used for installing the plugin on the backend servers rather than the proxy.

### Connection Pool

Includes whether the plugin has a connection pool *and* takes advantage of it.

AdvancedBan has a connection pool, but in practice, can only use 1 connection at a time, which is effectively the same as not using a connection pool.

### Server Scopes

This feature is relevant for proxies. Whether the plugin has the ability to define "scopes" and create punishments applying to certain scopes. This allows server administrators to create punishments applying to a specific backend server or a group of backend servers.

BanManager's *partial* ranking: BanManager has the ability to create a "local" punishment, meaning it applies to one backend server. However, it does not have the ability to define punishments applying to a group of backend servers.

### Uses UUIDs

Whether the plugin stores UUIDs instead of player names, for both the targets of punishments and operators of punishments. Player names can change, so they cannot be relied upon.

AdvancedBan's *partial* ranking: AdvancedBan uses UUIDs for the targets of punishments. However, it uses player names for operators.

### Schema Integrity

Whether the data types defined by the plugin's database schema have appropriate constraints and are of the correct type.

* When a database schema has integrity, a bug in the plugin will *not* corrupt user data.
* Otherwise, bugs in the plugin may corrupt user data.

Data corruption, if it occurs, is not easy to recover from and may require manually interfacing with the database.
