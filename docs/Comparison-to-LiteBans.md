Since LiteBans is closed-source and pay-walled, it is hard to gather information about its workings. Very little is known about its internal code design.

This comparison will focus on what information is known about LiteBans.

# Design

## API Design

LiteBans's API encourages users to query its database directly. While this makes the API comprehensive, it tightly couples LiteBans' database and API users. If an API user leaks open connections, for example, it will appear that LiteBans is the culprit of the leak.

LibertyBans discourages reliance on the database structure, and instead provides an expansive Java API which attempts to cover the capabilities offered by a punishment plugin. In contrast to LiteBans, simple plugins using the LibertyBans API can call type-safe methods instead of executing SQL with JDBC.

## Database schema

LiteBans, like other ban plugins, uses VARCHAR columns for storing UUIDs and IP addresses. This is equivalent to storing the string representation of UUIDs and IP addresses.

LibertyBans uses BINARY column types in order to reduce storage space. This means only the necessary bytes of the UUIDs and IP addresses are stored. The actual difference is small, but not negligible.

## Database Drivers

Both LiteBans and LibertyBans use modern JDBC drivers, which avoids issues with the default drivers which are on some servers quite old and bug-ridden. Other plugins use the default drivers, and thus experience issues LiteBans and LibertyBans do not.

# Requirements

## Java Version Support

LibertyBans requires Java 11 whereas LiteBans permits Java 8.

## External database

Neither LiteBans nor LibertyBans requires an external database. LibertyBans uses HSQLDB by default, and LiteBans uses H2 by default.

LibertyBans has support for MariaDB/MySQL. Note that you have to use a modern MySQL or MariaDB version. At least MySQL 8.0 or MariaDB 10.3 is required.

LiteBans supports MariaDB/MySQL and PostgreSQL. It also has support for SQLite, but SQLite usage is discouraged by LiteBans.

## Platform Support

LibertyBans supports Bukkit, Velocity, and BungeeCord.

LiteBans supports Bukkit and BungeeCord but does not provide a Velocity implementation. When using it with Velocity, LiteBans has to be installed on the backend servers.

# Philosophy

## Versioning

LiteBans does not follow semantic versioning, leading to potential instability for developers using its API. However, at least it has not yet introduced breaking API changes in the same major version, so LiteBans partially follows semver. Reference: https://www.spigotmc.org/resources/litebans.3715/update?update=341296

LibertyBans fully follows semantic versioning for its API.

## Price and Availability

LibertyBans is free and open-source. Anyone can inspect the source code if they so desire. Anyone can work on it; anyone can contribute and add features or modify it for own use. Users can use the latest version of the source code without having to wait until the next official release.

LiteBans is closed-source. Only the author has access to the source code; only the author can implement new features. The JAR is obfuscated so that it cannot be decompiled into readable source code.

LiteBans is also behind a pay-wall, meaning that users have to pay in order to use it. This also means that prospective users cannot test out the plugin so easily. They have to pay before testing.

# Features

## Geyser Support

LibertyBans has Geyser support (since 30 May 2021), whereas LiteBans does not.

## Core punishment types

Both LibertyBans and LiteBans include bans, ip-bans, mutes, warns, and kicks.

### IP-Mutes, IP-Warns, IP-Kicks

By the nature of its flexible design, LibertyBans also supports ip-mutes, ip-warns, and ip-kicks, even though these are rarely used. It costs nothing to add these extra features.

## Punishment enforcement

Both LibertyBans and LiteBans have a command to check for alts as well as an automatic notification when a suspected player joins.

LibertyBans further supports multiple modes of IP address-based punishment, in order to automatically block alt accounts.

## Exempt Permissions

LiteBans supports the feature known as exempt permissions, where a target player cannot be banned if they have a certain permission.

LibertyBans does not provide this feature.

## Importing

LibertyBans supports importing from AdvancedBan, LiteBans, and vanilla.

LiteBans supports importing from AdvancedBan, BanManager v4 and v5, BungeeAdminTools, MaxBans, UltraBans, and vanilla.

LiteBans also allows importing from itself, which LibertyBans does not currently support.

## Server Scopes

LiteBans has full support for punishments scoped to certain servers.

LibertyBans does not implement this feature.

## Multi-Proxy Support

LibertyBans has passive multi-instance support, which relies on the fact that it performs minimal caching.This has limitations, including that an operator on one server cannot punish a user on another, and have that punishment be enforced while that user is online. There is an [open feature request](https://github.com/A248/LibertyBans/issues/44) to improve this.

LiteBans synchronizes punishments across all of its instances, using SQL-based synchronization. This lets operators punish users on other servers with ease.



