# LibertyBans
The be-all, end-all of discipline.

## Introduction

### Design

* Effective and lightweight. No complicated installation and avoids unnecessary features.
* Option to use local file-based database (HyperSQL), or remote database (MariaDB/MySQL).
* Enhanced database-oriented performance emphasising calculations in SQL. Low memory usage because punishments are stored almost completely in the database.
* Compact storage definitions. UUIDs are stored in minimal form as raw bytes instead of strings. Same for IP addresses.
* Best practices for asynchronous calculations are followed. The performance cost of context switching is understood and avoided; the plugin does not blindly fire async tasks.
* Designed for high availability and concurrency. Minimal locking is employed while keeping state consistent; this is mostly realised through the fact that most plugin state is maintained in the database itself.
* Well-structured API providing a framework for other plugins to work with the plugin.

### Features

* Add and remove punishments:
    * /ban, /ipban, /unban, /unbanip - bans or unbans a player or IP address
    * /mute, /ipmute, /unmute, /unmuteip - mutes or unmutes a player or IP address
    * /warn, /ipwarn, /unwarn, /unwarnip - warns or unwarns a player or IP address
    * /kick, /ipkick - kicks a player or IP address
* All player data is stored using UUIDs. This isn't a silly plugin which stores data by player name.
* List punishments:
    * /banlist - shows all bans
    * /mutelist - shows all mutes
    * /history <player> - shows all punishments for a player
    * /warns <player> - shows all warns for a player
    * /blame <player> - shows all the punishments a staff member has enacted
* There is no /alts or /dupeip command. Why? Because this functionality is built into the enforcement of punishments. By default, alts are automatically banned from joining if the alt account's IP address matches that of a banned player.
* Options and behaviour are fully configurable. You can even tweak your connection pool and statement cache settings to fine-tune performance.
* All dependencies are automatically downloaded with secure SHA-512 hashes used to validate the downloads. Additionally, builds of LibertyBans are reproducible from the source code, meaning checksums can be personally verified.

## Basic Info

### Requirements

* Java 11 (or higher)
* A compatible server platform

### Supported Platforms

* Spigot/Paper or any forks thereof
* BungeeCord
* Velocity

### Why Java 11?

There have been several major improvements to Java for developers and server owners. Newer Java versions are faster for server owners and easier to develop with. I would strongly recommend updating to Java 11 or 14.

[Read more here](https://github.com/A248/LibertyBans/wiki/Why-Java-11%3F)

## Developer API

The developer API is extensive. LibertyBans does not recommend developers mess with the database as a first recourse. [More information here](https://github.com/A248/LibertyBans/wiki/Developer-API)

## Other Information

I am also a contributor to AdvancedBan. If you absolutely need Java 8 support I would recommend you use it. AdvancedBan has some problems with its design, and although I helped to improve it, there are some choices I needed to make differently in this plugin.

### License

LibertyBans is licensed under the GNU AGPL v3. See the license file for more information.
