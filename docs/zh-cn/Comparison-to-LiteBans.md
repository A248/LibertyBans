
Since LiteBans is closed-source and pay-walled, it is hard to gather information about its workings. Very little is known about its internal code design. In fact, it may even be illegal to reverse-engineer LiteBans.

This comparison will focus on what information is known about LiteBans.

## Design

### API

LiteBans's API encourages users to query its database directly. As a  result, developers working with the LiteBans API have criticized it as a poor abstraction. Requiring API users to execute SQL tightly couples the database schema and API users.

This has consequences:
* If a plugin leaks a connection, it will appear that LiteBans is the source of the leak. Debugging connection leaks is further complicated because the LiteBans binary is obfuscated.
* Creating a rigid database schema upon which API users rely means LiteBans is unable to improve its database schema without widespread breakage.

LibertyBans instead provides an expansive Java API which attempts to cover the capabilities offered by a punishment plugin. In contrast to LiteBans, plugins using the LibertyBans API need not execute SQL.

### Database Schema

LiteBans, like other ban plugins, uses VARCHAR columns for storing UUIDs and IP addresses. This is equivalent to storing the string representation of UUIDs and IP addresses.

LibertyBans uses BINARY column types in order to reduce storage space. This means only the necessary bytes of the UUIDs and IP addresses are stored. The actual difference is small, but not negligible.

### Undoing Operator

LiteBans stores the operator who revoked a punishment. This makes it possible for staff to determine who is responsible for unbans, unmutes, etc.

LibertyBans does not store this information.

## Implementation

### Test Suite

LibertyBans has an extensive test suite. Automated tests help catch bugs before a release can be made. The scope and strength of automated testing in LibertyBans has saved countless hours of development time.

LiteBans is closed-source; as such, it is not clear whether it has a test suite. However, evidence suggests LiteBans has little automated testing:
* LiteBans has experienced bugs which are of such a kind as to imply LiteBans does not have significant automated testing:
  * "Fixed the /unwarn command, broken since 2.1" and "Fixed a harmless error when starting the plugin for the first time if config.yml doesn't exist yet" - https://www.spigotmc.org/resources/litebans.3715/update?update=102167
  * "Fixed Database.prepareStatement() returning a closed statement" - https://www.spigotmc.org/resources/litebans.3715/update?update=163048
  * A bug due to an invalid query string: https://gitlab.com/ruany/LiteBans/-/issues/391
  * These kinds of bugs would most likely be prevented by automated testing
* Bug descriptions frequently mention manual testing, but none of them mention automated testing.

### Platform Separation

The LibertyBans codebase is separated based on the platform.

Separating code for each platform prevents whole categories of bugs relating to accidental class initialization. Fewer bugs means less time spent debugging and more time available for improving the rest of the plugin.

Although LiteBans is closed-source, evidence strongly suggests that its codebase is *not* separated for each platform.<sup id="note1ret">[1](#note1)</sup>

## Philosophy

### Price and Availability

LibertyBans is free and open-source. Anyone can inspect the source code if they so desire. Anyone can work on it; anyone can contribute and add features or modify it for own use. Users can use the latest version of the source code without having to wait until the next official release.

LiteBans is closed-source. Only the author has access to the source code; only the author can implement new features. The JAR is obfuscated so that it cannot be decompiled into readable source code.

LiteBans is also behind a pay-wall, meaning that users have to pay in order to use it. This also means that prospective users cannot test out the plugin so easily. They have to pay before testing.

Users of LiteBans must carefully consider whether they are willing to rely entirely on the author of LiteBans for features and improvements.

### Support

Support for LibertyBans is provided through GitHub Issues and a dedicated text channel on a Discord Server.

LiteBans primarily offers support through Discord; however, it only does so for users who verified their purchase of the plugin. This also includes requesting support for their API, meaning if you're not a customer of their plugin, chances are low you will gain access to their support server, or receive help with their API at all.

### Versioning

LiteBans does not follow semantic versioning, leading to potential instability for developers using its API. It has introduced new API without issuing a minor release.<sup id="note2ret">[2](#note2)</sup>

LibertyBans fully follows semantic versioning for its API.

## Requirements

### Java Version Support

LibertyBans requires Java 17 whereas LiteBans permits Java 8.

### External Databases

Neither LibertyBans nor LiteBans requires an external database. LibertyBans uses HSQLDB by default, and LiteBans uses H2 by default.

Both LibertyBans and LiteBans support MariaDB, MySQL, and PostgreSQL. LiteBans also has support for SQLite, but SQLite usage is discouraged by LiteBans.

LibertyBans requires certain minimum versions for database servers. At least MySQL 8.0, MariaDB 10.3, or PostgreSQL 12 is required. Older versions are not supported.

## Platform Support

* Bukkit, BungeeCord, and Velocity are supported by both plugins.
* Sponge:
  * LibertyBans supports Sponge.
  * LiteBans declines to support Sponge on the author's reason that Sponge does not provide asynchronous chat events.<sup id="note3ret">[3](#note3)</sup>

LiteBans' support for Velocity came after repeated user requests. It was suggested that LiteBans be made open-source so that someone could contribute a Velocity version. The reluctance of the LiteBans author to add Velocity support suggests that it may be unwise to rely on proprietary software for critical functionality.

## Features

### Geyser Support

LibertyBans and LiteBans have Geyser support.

However, documentation suggests LiteBans requires the prefix to be the period character (".").<sup id="note4ret">[4](#note4)</sup>

### Core punishment types

Both LibertyBans and LiteBans include bans, ip-bans, mutes, warns, and kicks.

By the nature of its flexible design, LibertyBans also supports ip-mutes, ip-warns, and ip-kicks, even though these are rarely used. It costs nothing to add these extra features.

LiteBans allows banning IP ranges. LibertyBans does not.

### Combatting Punishment Evasion (Alt Accounts)

Both LibertyBans and LiteBans have a command to check for alts as well as an automatic notification when an alt of a banned player joins the server.

LibertyBans further supports multiple modes of IP address-based punishment, in order to automatically block alt accounts. LiteBans does not have this feature.

### Exemption

Although both plugins support the exemption feature which prevents staff from banning each other, it is implemented slightly differently.

For offline players, LiteBans's permission checking depends on Vault on single servers. Without a Vault-compatible permissions plugin, the feature breaks silently for offline players.

LibertyBans' exemption feature will never break silently. However, it requires the installation of a supported exemption provider - currently LuckPerms or Vault. Without an exemption provider, the feature is entirely unavailable.

Moreover, LiteBans' exemption does not provide arbitrary levels.<sup id="note5ret">[5](#note5)</sup>

### Importing From Other Plugins

LibertyBans supports importing from AdvancedBan, BanManager, LiteBans, and vanilla.

LiteBans supports importing from AdvancedBan, BanManager, BungeeAdminTools, MaxBans, UltraBans, and vanilla.

### Server Scopes

Both plugins enable punishments scoped to certain servers.

However, LiteBans lacks scope categories, or the ability to group servers into a single scope.<sup id="note6ret">[6](#note6)</sup>

### Multi-Proxy / Multi-Instance Synchronization

Both LibertyBans and LiteBans provide synchronization across multiple instances, a feature commonly used for multi-proxy setups.

## References

<a id="note1">1</a>: Falistos. "Start error under Java 18." LiteBans Gitlab Issue. https://gitlab.com/ruany/LiteBans/-/issues/408 [↩](#note1ret)

<a id="note2">2</a>: Ruan. "LiteBans 2.5.4 - 2.5.9." SpigotMC Resource Update. https://www.spigotmc.org/resources/litebans.3715/update?update=341296 [↩](#note2ret)

<a id="note3">3</a>: Ruan. "[Feature] Spongepowered?". LiteBans Gitlab Issue Comment. https://gitlab.com/ruany/LiteBans/-/issues/41#note_324182783 [↩](#note3ret)

<a id="note4">4</a>: Ruan. "LiteBans 2.7.5 (Connection error fix)". SpigotMC Resource Update. https://www.spigotmc.org/resources/litebans.3715/update?update=414133 [↩](#note4ret)

<a id="note5">5</a>: Ruan. "LiteBans Exempt". LiteBans Gitlab Issue Comment. https://gitlab.com/ruany/LiteBans/-/issues/223 [↩](#note5ret)

<a id="note6">6</a>: lewisakura. "Server scope groups". LiteBans Gitlab Issue. https://gitlab.com/ruany/LiteBans/-/issues/452 [↩](#note6ret)

### Disclaimer

Please note that no harm is meant to subjects of criticism. If the writing sounds harsh, we apologize; please let us know and we will make the language less harsh.
