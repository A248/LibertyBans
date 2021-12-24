This is a quick table for comparing between LibertyBans, AdvancedBan, and LiteBans.

**NB: *Tables and graphs do not tell the full picture!*** To really understand what's going on here, read the detailed and explanatory 
comparisons. You can find these on the sidebar at the right. For each plugin here, there is a detailed comparison between LibertyBans and the plugin.

### Table

| Plugin      | Supported Platforms          | Java Req. | Free | Open-Source | Database Support                                             | Thread-Safe Design | Semver API | Geyser Support | Multi-Proxy Support | Connection Pool* | Exempt Permissions | Server Scopes | Uses UUIDs | Import From                                                                      |
|-------------|------------------------------|-----------|------|-------------|--------------------------------------------------------------|--------------------|------------|----------------|---------------------|------------------|--------------------|---------------|------------|----------------------------------------------------------------------------------|
| LibertyBans | Bukkit, BungeeCord, Velocity | 11+       | ✔️   | ✔️          | HSQLDB (local), MariaDB, MySQL, PostgreSQL                   | ✔️                 | ✔️         | ✔️             | ✔️                  | ✔️               | ❌                  | ❌             | ✔️         | AdvancedBan, LiteBans, vanilla                                                   |
| AdvancedBan | Bukkit, BungeeCord, Velocity | 8+        | ✔️   | ✔️          | HSQLDB (local), MariaDB/MySQL                                | ❌                  | ❌          | ❓              | ❌                   | ❌                | ✔️                 | ❌             | ➖          | _                                                                                |
| LiteBans    | Bukkit, BungeeCord           | 8+        | ❌    | ❌           | H2 (local), MariaDB, MySQL, PostgreSQL, SQLite (discouraged) | ❓                  | ➖          | ❌              | ✔️                  | ❓                | ✔️                 | ✔️            | ✔️         | AdvancedBan, BanManager v4 and v5, BungeeAdminTools, MaxBans, UltraBans, vanilla |

`*` Includes whether the plugin has a connection pool *and* takes advantage of it. For example, AdvancedBan has a connection pool, but in practice, can only use 1 connection at a time, which is effectively the same as not using a connection pool.

Legend:

✔️ – Yes

➖ – Partially

❌ – No

❓ - Unknown, either because the plugin is closed-source, or the feature has not been tested.
