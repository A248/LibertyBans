<!--suppress HtmlDeprecatedAttribute -->
<div align="center">
<img alt="LibertyBans" src="./.github/banner.png" />

<!-- Shields -->
[Homepage]:https://img.shields.io/badge/-Home_Page-blueviolet.svg?logo=StarTrek&style=for-the-badge&logoColor=white
[Documentation]:https://img.shields.io/badge/-Documentation-blue.svg?logo=Wikipedia&style=for-the-badge&logoColor=black
[Discord]:https://img.shields.io/badge/-Discord-5865F2.svg?logo=discord&style=for-the-badge&logoColor=white
[Spigot]:https://img.shields.io/badge/-SpigotMC-ef9023.svg?logo=Accenture&style=for-the-badge&logoColor=grey

[SpigotRating]:https://img.shields.io/spiget/rating/81063?style=flat-square
[GitHubStar]:https://img.shields.io/github/stars/A248/LibertyBans
[TestedVersion]: https://img.shields.io/spiget/tested-versions/81063?label=Tested%20on&style=flat-square
[License]:https://img.shields.io/github/license/A248/LibertyBans
[CodeSize]:https://img.shields.io/github/languages/code-size/A248/LibertyBans
<!-- Shields -->
[![Homepage]](https://libertybans.org)

[![Discord]](https://discord.gg/3C4qeG8XhE)
[![Documentation]](https://docs.libertybans.org/#/Getting-Started)
[![Spigot]](https://spigotmc.org/resources/81063)
<br><br>[![TestedVersion]](https://spigotmc.org/resources/81063) ![SpigotRating]
<br>![License] ![GitHubStar] ![CodeSize]
</div>

## Table of Contents
* [Introduction](#introduction)
  * [Design](#design)
  * [Features](#features)
* [Basic Info](#basic-info)
* [Developer API](#developer-api)

## Introduction

Free software and high quality, LibertyBans is the best-designed punishment plugin. It can efficiently scale to large networks and operate on single servers with minimal resources.

### Design

* Effective and lightweight. No complicated installation and avoids unnecessary features.
* Option to use local file-based database (HyperSQL), or remote database (MariaDB, MySQL, PostgreSQL).
* Enhanced database-oriented performance emphasising calculations in SQL. Low memory usage because punishments are stored almost completely in the database. Data is stored in minimal form as raw bytes instead of strings.
* Best practices for asynchronous calculations are followed. The performance cost of context switching is understood and avoided; the plugin does not blindly fire async tasks.
* Designed for high availability and concurrency. Minimal locking is employed while keeping state consistent; this is mostly realised through the fact that most plugin state is maintained in the database itself.
* Well-structured API providing a framework for other plugins to work with the plugin.

### Features

* Add and remove punishments:
    * /ban, /ipban, /unban, /unbanip - bans or unbans a player or IP address
    * /mute, /ipmute, /unmute, /unmuteip - mutes or unmutes a player or IP address
    * /warn, /ipwarn, /unwarn, /unwarnip - warns or unwarns a player or IP address
    * /kick, /ipkick - kicks a player or IP address
* Temporary versions of bans, mutes, and warns
* All player data is stored using UUIDs. This isn't a silly plugin which stores data by player name.
* List punishments:
    * /banlist - shows all bans
    * /mutelist - shows all mutes
    * /history <player> - shows all punishments for a player
    * /warns <player> - shows all warns for a player
    * /blame <player> - shows all the punishments a staff member has enacted
* Multiple means to block alt accounts:
  * Automatic enforcement to block alt accounts. *By default, when using IP-bans, alts are automatically banned from joining if the main account is banned.* This behavior is configurable, and you can even increase the strictness of alt-checking.
  * Manual /alts command - shows suspected alt accounts for a player.
  * Alts notification on join - tells staff members when a player whose suspected alt is banned or muted.
* Exemption. For example, trainees cannot ban admins; admins cannot ban owners.
* Options and behaviour are fully configurable. You can even tweak your connection pool and statement cache settings to fine-tune performance.
* Full multi-proxy and multi-instance support. You can place LibertyBans on the proxy if you use a single proxy, or on multiple backend servers.
* All dependencies are automatically downloaded with secure SHA-512 hashes used to validate the downloads. Additionally, builds of LibertyBans are reproducible from the source code, meaning checksums can be personally verified.

## Basic Info

### Requirements

* Java 17

Supported platforms:

* Spigot / Paper (+Folia)
* BungeeCord / Waterfall
* Sponge
* Velocity

Compatibility with Geyser/Floodgate usernames.

### Installation

LibertyBans will work out-of-the-box for most users.

* When using a single proxy, it is recommended, but not required, to install LibertyBans on the proxy itself.
* LibertyBans can also be installed on the backend servers if you are willing to configure multi-instance synchronization.

For installing on the backend servers and synchronizing punishments, see the wiki for additional information.

### Developer API

The developer API is extensive. LibertyBans does not recommend developers mess with the database as a first recourse. [More information here](https://docs.libertybans.org/#/Developer-API)

### License

LibertyBans is licensed under the GNU AGPL v3. See the license file for more information.

[![GNU AGPL Logo](https://www.gnu.org/graphics/agplv3-155x51.png)](https://www.gnu.org/licenses/agpl-3.0.en.html)
