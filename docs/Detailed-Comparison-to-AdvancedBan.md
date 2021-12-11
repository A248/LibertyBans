This is intended to be an impartial analysis of the API, design, and implementation of AdvancedBan compared to LibertyBans.

As one of the lead contributors to AdvancedBan and the creator of LibertyBans, I am not completely unbiased. However, this means I am familiar with both codebases.

# Design

## Singleton Managers

AdvancedBan's codebase is heavily oriented toward singletons. This design decision leads to very fragile initialization. In the past, there have been issues where another plugin referenced AdvancedBan's classes and initialized them prematurely.

LibertyBans is fully instance-based and more flexible in this regard. It is even possible to create multiple instances of it. Another plugin referencing its classes will *not* cause initialization to blow up.

## Caching

AdvancedBan uses a hand-rolled punishment cache. This cache is quite brittle and has resulted in several annoying bugs.

LibertyBans uses a popular and well-tested caching library, called [Caffeine](https://github.com/ben-manes/caffeine). This library is used in software infrastructure in all kinds of applications. This means development of LibertyBans can focus on more important features.

Moreover, LibertyBans only caches what is necessary - mutes. AdvancedBan caches all punishments of online players, which results in somewhat more memory usage.

## Concurrency

### Synchronous / Asynchronous Constructs

Unlike in popular myth, threads are not cheap. At present, a thread in Java is a 1-to-1 mapping of an OS thread, which requires significant resources. A wise program avoids context switches for small workloads, since the overhead of multithreaded execution would exceed the gains of using multiple cores.

The AdvancedBan internals and API are synchronously designed. As a result, AdvancedBan necessarily spawns a new threaded task for most operations. This leads to increased context switching and resource consumption.

LibertyBan's asynchronous design has a few advantages. Threads are created as necessary and in accordance with the workload. This is much more lightweight and scalable.

Additionally, it is easier for developers of AdvancedBan to accidentally call a heavy operation on the main thread. Small errors can easily make it into the released JAR. A more robust internal code structure means LibertyBans does not suffer this possibility.

Note that *synchronous*, in this context, does **not** mean "on the main thread". Asynchronous does **not** mean multithreaded. For a full explanation of the meaning of these terms, see https://stackoverflow.com/a/748235/

### Thread safety

Parts of AdvancedBan's code are not thread safe. This has lead to many subtle bugs, which are still plaguing AdvancedBan. Moreover, AdvancedBan does not place much of an emphasis on synchronisation.

In LibertyBans, there have never been any known synchronisation issues to-date, which is impressive considering how difficult concurrency is to get right. The strongest effort is made to ensure thread safety at every step. Luckily, the database-driven design reduces much of the statefulness of the plugin.

## Timezone usage

AdvancedBan stores the timezone offset as part of the timestamp in the database. This is somewhat clunky, and it requires that users of the AdvancedBan API carefully follow suit with this little-documented behavior.

LibertyBans uses UTC time in the database. When displaying a punishment, it uses the configured timezone.

# Other Details

## Connection Pooling

AdvancedBan depends on HikariCP since version 2.1.9, so it looks like it uses a connection pool. However, in reality, internal synchronization in AdvancedBan prevents it from taking advantage of the additional connections. Unfortunately, this is not so easy to solve on AdvancedBan's side. That same synchronization is critical for preventing other thread safety problems. Reference: https://github.com/DevLeoko/AdvancedBan/pull/393

LibertyBans uses HikariCP and takes advantage of it.

## SQL Table Definitions

AdvancedBan, like other ban plugins, uses VARCHAR columns for storing UUIDs and IP addresses. This is equivalent to storing the string representation of UUIDs and IP addresses.

LibertyBans uses BINARY column types in order to reduce storage space. This means only the necessary bytes of the UUIDs and IP addresses are stored. The actual difference is small, but not negligible.

## Database Drivers

AdvancedBan uses the server's MySQL Connector-J driver. Other plugins do this as well. However, on some server versions, the Connector-J driver version is very old, with bugs which are fixed in later versions.

LibertyBans uses a modern MariaDB-Connector version, which is compatible with MySQL and MariaDB and does not have the bugs present from ancient Connector-J versions.

## UTF-8 Encoding

AdvancedBan has historically suffered with UTF-8 support, and the current AdvancedBan codebase, which still uses the default encoding in some places, suggests there are still issues. UTF-8 has been problematic in config files as well as database definitions. References: https://www.spigotmc.org/resources/advancedban.8695/update?update=327430 , https://www.spigotmc.org/resources/advancedban.8695/update?update=228946 , https://github.com/DevLeoko/AdvancedBan/issues/433 , https://github.com/DevLeoko/AdvancedBan/issues/430 , https://github.com/DevLeoko/AdvancedBan/issues/202 , and https://github.com/DevLeoko/AdvancedBan/issues/74

LibertyBans is made with full Unicode support in mind.

# Philosophy

## Versioning Policy

LibertyBans follows strict semantic versioning. This means that updates a single major release (e.g. 1.x or 2.x) will not introduce breaking changes. Semantic versioning makes developing dependent plugins much easier, because it ensures compatibility with a clearly stated number. It means updating the plugin within the same major release won't break your setup.

AdvancedBan does not follow semver. On the contrary, it has made breaking changes within minor versions. For example, 2.3.0 is not guaranteed to be compatible with 2.2.1. As another example, version 2.1.6 changed the format of notify permissions.

The purpose of semantic versioning is to avoid confusion and dependency conflicts by clearly defining compatibility. AdvancedBan's refusal to follow it means code made with a previous version of AdvancedBan could break after a seemingly small update.

References: https://www.spigotmc.org/resources/advancedban.8695/update?update=367825 and https://www.spigotmc.org/resources/advancedban.8695/update?update=321631

## Price

Both AdvancedBan and LibertyBans are free and open-source.

In the past, there was an update of AdvancedBan where **the author charged users to upgrade their data**. The update changed the plugin's data format but AdvancedBan did not provide a free migrator for users on previous versions. Reference: https://www.spigotmc.org/resources/advancedban.8695/update?update=180773

## User Philosophy

There is an increased tendency to cater to users' every wish in AdvancedBan. In LibertyBans, there is more of an emphasis on *doing things correctly* than making things easy. Cutting corners often leads to problems.

An example of this difference in philosophy is a bug in AdvancedBan which causes it to take over other plugins' commands. For example, if you are running Essentials, `/essentials:ban` may actually become an AdvancedBan command! This is confusing behavior. It leads to silly error messages such as:

> Unhandled exception during tab completion for command '/tempban Siikeee ' in plugin Essentials v2.18.1.0

Even though it is AdvancedBan which is managing the /tempban command. References: https://pastebin.com/v46X9J2M and https://github.com/DevLeoko/AdvancedBan/pull/373

LibertyBans believes in better stewardship and will not take over other plugins' commands. If there is a conflicting command, users should solve the conflict through the correct means (using the server's commands.yml, defining aliases, etc.).

# Requirements

In general, LibertyBans uses more modern technology, but also has greater requirements.

## Java Version Support

LibertyBans requires Java 11 whereas AdvancedBan permits Java 8.

## External database (MariaDB/MySQL)

Using MySQL in either plugin is optional. Both AdvancedBan and LibertyBans use HSQLDB by default.

In LibertyBans, if you choose MariaDB or MySQL, you have to use a modern MySQL or MariaDB version. At least MySQL 8.0 or MariaDB 10.3 is required.

## Platform Support

Both AdvancedBan and LibertyBans support the same platforms. AdvancedBan had velocity support recently merged (24 January 2021)

# Features

AdvancedBan has more features. It has punishment layouts and player notes, for example.

LibertyBans takes a more minimalist approach and avoids features it deems unnecessary.

## Core punishment types

Both LibertyBans and AdvancedBan include bans, ip-bans, mutes, warns, and kicks.

### Player Notes

AdvancedBan supports player notes, whereas LibertyBans does not.

### IP-Mutes, IP-Warns, IP-Kicks

By the nature of its flexible design, LibertyBans also supports ip-mutes, ip-warns, and ip-kicks, even though these are rarely used. It costs nothing to add these extra features.

## Punishment enforcement

LibertyBans supports multiple modes of IP address-based punishment, in order to automatically block alt accounts. AdvancedBan has no similar functionality.

AdvancedBan has a command to check a player's IP address, but not the ability to scan for alts. LibertyBans has less of a need for a manual alt-check command, but there is an [open feature request](https://github.com/A248/LibertyBans/issues/34) for it.

## Exempt Permissions

AdvancedBan supports a feature known as exempt permissions, where a target player cannot be banned if they have a certain permission. Note that on a proxy, permissions checking for offline players depends on LuckPerms, otherwise the feature breaks.

LibertyBans does not provide this feature.

## Importing

LibertyBans supports importing from AdvancedBan, LiteBans, and vanilla.

AdvancedBan does not provide an importer.

## Punishment Listing

AdvancedBan has /banlist, which is really a punishment list which includes all active punishments.

LibertyBans' /banlist only shows bans, while /mutelist only shows mutes.

Both plugins provide /history and /warns in the same manner.

LibertyBans also includes /blame, which AdvancedBan does not.
