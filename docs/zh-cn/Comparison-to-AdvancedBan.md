
This is intended to be an impartial analysis of the API, design, and implementation of AdvancedBan compared to LibertyBans.

I (A248) am both the creator of LibertyBans and one of the lead contributors to AdvancedBan. Although I am not unbiased, I rely on my familiarity with both codebases to conduct an in-depth analysis.

## Design

### Singleton Managers

AdvancedBan's codebase is heavily oriented toward singletons. This design decision leads to very fragile initialization. In the past, there have been issues where another plugin referenced AdvancedBan's classes and initialized them prematurely.<sup id="note1ret">[1](#note1)</sup>

LibertyBans is fully instance-based, which means another plugin referencing its classes will not cause initialization conflicts.

### IDs

AdvancedBan has a quirk where it creates two IDs for every punishment - one ID for the active punishment and another ID for the punishment in history.

This is quite counter-intuitive. Having two separate punishment IDs can lead to confusion with appeals systems and variables in message configuration.

This finding was so unexpected that some of the core contributors to AdvancedBan were not aware of it and were surprised to hear about it.<sup id="note2ret">[2](#note2)</sup>

### Operator UUIDs

AdvancedBan stores operator names, but not UUIDs. If a server operator changes their name, AdvancedBan will continue to display the old name in punishment messages.

LibertyBans stores the operator UUID and displays the most recently-known name.<sup id="note3ret">[3](#note3)</sup>

### Database Schema

AdvancedBan uses VARCHAR columns for storing UUIDs and IP addresses. This is equivalent to storing the string representation of UUIDs and IP addresses. In contrast, LibertyBans uses BINARY column types in order to reduce storage space. The actual difference is small, but not negligible.

### Timezones

AdvancedBan stores the timezone offset as part of the timestamp in the database. This is somewhat clunky, and it requires that users of the AdvancedBan API carefully follow suit with this little-documented behavior.

LibertyBans uses UTC time in the database. When displaying a punishment, it uses the configured timezone.

Consequently, if you change the configured timezone in LibertyBans, all punishments will transparently update their displayed time. In AdvancedBan, however, the time will not be updated to the new timezone.

## Implementation

### Caching

AdvancedBan uses a hand-rolled punishment cache. This cache is quite brittle and has resulted in several bugs. It is populated and invalidated manually. Users ocassionally report issues with cached punishments failing to be invalidated.

LibertyBans caches only mutes, and nothing else. It uses a caching library. No bugs to date (as of 21 Dec 2021) have been observed with the caching mechanism used by LibertyBans.

### Transactional Consistency

AdvancedBan does not rely on transactional consistency like LibertyBans does. In practice, this means certain conditions can be violated in certain circumstances in AdvancedBan - this is a bug in AdvancedBan's design, and it is not a simple fix.

In both AdvancedBan and Liberty*Bans, attempting to ban a player already banned results in a message affirming such. If you try to ban another player twice, AdvancedBan will tell you "this player is already banned." However, this guarantee does not hold solid. If you are somehow able to execute /ban quickly enough twice, you can successfully ban another player twice.<sup id="note4ret">[4](#note4)</sup>

Although it may sound far-fetched to ban a player twice quickly, it has been observed multiple times in practice. Automated punishment mechanisms, such as anticheats, are prone to issue commands very quickly, as they are not limited by human typing speed.

This bug in AdvancedBan used to be one reason users of LibertyBans who had imported from AdvancedBan would observe duplicate punishments. In contrast to AdvancedBan, LibertyBans makes full use of transactional consistency, and is therefore not subjet to the described AdvancedBan bug.

### Synchronous / Asynchronous Constructs

Unlike in popular myth, threads are not cheap. At present, a thread in Java is a 1-to-1 mapping of an OS thread, which requires significant resources. A wise program avoids context switches for small workloads, since the overhead of multithreaded execution would exceed the gains of using multiple cores.

The AdvancedBan internals and API are synchronously designed. As a result, AdvancedBan necessarily spawns a new threaded task for most operations. This leads to increased context switching and resource consumption.

LibertyBan's asynchronous design has a few advantages. Threads are created as necessary and in accordance with the workload. This is much more lightweight and scalable.

As a another consequence, it is easier for developers of AdvancedBan to accidentally introduce bugs. Specifically, it becomes easier to call a heavy operation on the main thread. Indeed, AdvancedBan will perform a query on the main thread in limited circumstances.<sup id="note5ret">[5](#note5)</sup>

Note that *synchronous*, in this context, does **not** mean "on the main thread". Asynchronous does **not** mean multithreaded. I suggest reading [this StackOverflow answer](https://stackoverflow.com/a/748235/).

### Thread safety

Parts of AdvancedBan's code are not thread safe. This has lead to many subtle bugs, which are still plaguing AdvancedBan.<sup id="note6ret">[6](#note6)</sup> Moreover, AdvancedBan does not place much of an emphasis on synchronisation.<sup id="note7ret">[7](#note7)</sup>

In LibertyBans, there have never been any known synchronisation issues to-date.

### Connection Pooling

AdvancedBan depends on HikariCP since version 2.1.9, so it looks like it uses a connection pool. However, in reality, internal synchronization in AdvancedBan prevents it from taking advantage of the additional connections. Unfortunately, this is not so easy to solve on AdvancedBan's side. That same synchronization is critical for preventing other thread safety problems.<sup id="note8ret">[8](#note8)</sup>

LibertyBans uses HikariCP and takes advantage of it.

### UTF-8 Encoding

AdvancedBan has historically suffered with UTF-8 support, and the current AdvancedBan codebase still uses the default encoding in some places, which suggests there are still lurking issues. UTF-8 has been problematic in config files as well as database definitions. References: 
 * https://www.spigotmc.org/resources/advancedban.8695/update?update=327430 
 * https://www.spigotmc.org/resources/advancedban.8695/update?update=228946
 * https://github.com/DevLeoko/AdvancedBan/issues/433
 * https://github.com/DevLeoko/AdvancedBan/issues/430
 * https://github.com/DevLeoko/AdvancedBan/issues/202
 * https://github.com/DevLeoko/AdvancedBan/issues/74

LibertyBans is made with full Unicode support in mind.

### API Documentation

The AdvancedBan API is significantly under-documented. This makes it time-consuming for other developers to use the AdvancedBan API, as they need to discover the meaning and details of API methods by looking at the implementation or asking for support. Details such as nullability and contractual guarantees are wholly missing from the AdvancedBan API documentation.

The lack of a clear API specification in AdvancedBan also leads to incorrect assumptions: by relying on certain implementation details, external code becomes reliant on specific behavior which the author of AdvancedBan may not have intended such behavior to be guaranteed. In turn, this means that a future update of AdvancedBan may inadvertently break plugins which depended on the specific internal behavior.

In contrast, the LibertyBans API is fully documented using javadocs. This makes it easy for other developers to learn and use the LibertyBans API. Method expectations, contracts, and guarantees are clearly stated and explained.

## Philosophy

### Versioning Policy

LibertyBans follows strict semantic versioning. This means that updates a single major release (e.g. 1.x or 2.x) will not introduce breaking changes. Semantic versioning makes developing dependent plugins much easier, because it ensures compatibility with a clearly stated number. It means updating the plugin within the same major release won't break your setup.

AdvancedBan does not follow semver. On the contrary, it has made breaking changes within minor versions. For example, 2.3.0 is not guaranteed to be compatible with 2.2.1. As another example, version 2.1.6 changed the format of notify permissions.

The purpose of semantic versioning is to avoid confusion and dependency conflicts by clearly defining compatibility. AdvancedBan's refusal to follow it means code made with a previous version of AdvancedBan could break after a seemingly small update.

References:
  * https://www.spigotmc.org/resources/advancedban.8695/update?update=367825
  * https://www.spigotmc.org/resources/advancedban.8695/update?update=321631

### Price

Both AdvancedBan and LibertyBans are distributed at no cost.

In the past, there was an update of AdvancedBan where **the author charged users to upgrade their data**. The update changed the plugin's data format but AdvancedBan did not provide a free migrator for users on previous versions.<sup id="note9ret">[9](#note9)</sup>

### User Philosophy

There is an increased tendency to cater to users' every wish in AdvancedBan. In LibertyBans, there is more of an emphasis on *doing things correctly* than making things easy. Cutting corners often leads to problems.

An example of this difference in philosophy is a bug in AdvancedBan which causes it to take over other plugins' commands. For example, if you are running Essentials, `/essentials:ban` may actually become an AdvancedBan command! This is confusing behavior. It leads to silly error messages such as:

> Unhandled exception during tab completion for command '/tempban Siikeee ' in plugin Essentials v2.18.1.0

Even though it is AdvancedBan which is managing the /tempban command.<sup id="note10ret">[10](#note10)</sup> See also [the full error](https://pastebin.com/v46X9J2M).

LibertyBans believes in better stewardship and will not take over other plugins' commands. If there is a conflicting command, users should solve the conflict through the correct means (using the server's commands.yml, defining aliases, etc.).

## Requirements

In general, LibertyBans uses more modern technology, but also has greater requirements.

### Java Version Support

LibertyBans requires Java 17 whereas AdvancedBan permits Java 8.

### External Databases

Using an external database in either plugin is optional. Both AdvancedBan and LibertyBans use HSQLDB by default.

LibertyBans requires certain minimum versions for database servers. At least MySQL 8.0, MariaDB 10.6, or PostgreSQL 12 is required. Older versions are not supported.

### Platform Support

LibertyBans supports Sponge and Velocity, which AdvancedBan does not. Both support Bukkit and BungeeCord.

Note that AdvancedBan has "unstable" Velocity support, but the feature has known bugs. Contributors say it is not maintained.<sup id="note11ret">[11](#note11)</sup>

## Features

AdvancedBan and LibertyBans each have several features the other does not.

### Core punishment types

* Both LibertyBans and AdvancedBan include bans, ip-bans, mutes, warns, and kicks.
* AdvancedBan supports player notes, whereas LibertyBans does not.
* By the nature of its flexible design, LibertyBans also supports ip-mutes, ip-warns, and ip-kicks, even though these are rarely used. It costs nothing to add these extra features.

### Combatting Punishment Evasion (Alt Accounts)

LibertyBans supports multiple modes of IP address-based punishment, in order to automatically block alt accounts. There is also a command to check for alts, as well as an automatic notification when an alt of a banned player joins the server.

AdvancedBan has no similar functionality. It does have a command to check a player's IP address, however.

### Exemption

Although both plugins support the exemption feature which prevents staff from banning each other, it is implemented slightly differently.

For offline players, AdvancedBan's permission checking depends on LuckPerms on proxies and Vault permissions on single servers. If these conditions are not met, the feature breaks silently for offline players.

LibertyBans' exemption feature will never break silently. However, this imposes greater requirements. LibertyBans requires the installation of a supported exemption provider - currently LuckPerms or Vault. Without an exemption provider, the feature is entirely unavailable.

### Importing From Other Plugins

LibertyBans supports importing from AdvancedBan, BanManager, LiteBans, and vanilla.

AdvancedBan does not provide any importers.

### Converting Storage Backends

LibertyBans allows the user to swap between storage backends using the self-import feature.

AdvancedBan does not provide this feature itself, but [an extension plugin](https://github.com/A248/AdvancedBanMigrator) implements this conversion.

### Punishment Listing

AdvancedBan has /banlist, which is really a punishment list which includes all active punishments.

LibertyBans' /banlist only shows bans, while /mutelist only shows mutes.

Both plugins provide /history and /warns in the same manner.

LibertyBans also includes /blame, which AdvancedBan does not.

### Multi-Proxy / Multi-Instance Synchronization

LibertyBans synchronizes punishments across multiple instances, if enabled. This is commonly used for multi-proxy setups.

### Layouts

AdvancedBan has a feature called layouts which specifies pre-defined reasons for use by staff.

## References

<a id="note1">1</a>: A248. "Singleton initialization beginning to show its weakness." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/406 [↩](#note1ret)

<a id="note2">2</a>: A248. Discord message in Leoko.dev Discord guild. https://discord.com/channels/339110599084736522/380068075610963968/923016591585726554 [↩](#note2ret)

<a id="note3">3</a>: jeeukko. "Store UUID of punisher in the database." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/435 [↩](#note3ret)

<a id="note4">4</a>: A248. "Race condition between adding new bans and checking if the user is already banned." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/541 [↩](#note4ret)

<a id="note5">5</a>: A248. "Database Queries on the Main Thread." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/458 [↩](#note5ret)

<a id="note6">6</a>: AerWyn81. "ConcurrentModificationException on tempban command executed by another plugin." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/462 [↩](#note6ret)

<a id="note7">7</a>: Leoko. Discord message in Leoko.dev Discord guild. https://discord.com/channels/339110599084736522/339110599084736522/745057107769819267 [↩](#note7ret)

<a id="note8">8</a>: A248. "Database queries need not block each other." AdvancedBan Pull Request. https://github.com/DevLeoko/AdvancedBan/pull/393 [↩](#note8ret)

<a id="note9">9</a>: Leoko. "AdvancedBan [1.7-1.12] v2.1.0 | Performance update." SpigotMC Resource Update. https://www.spigotmc.org/resources/advancedban.8695/update?update=180773 [↩](#note9ret)

<a id="note10">10</a>: A248. "Friendly Register Commands Config Option. AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/pull/373 [↩](#note10ret)

<a id="note11">11</a>: Hopefuls. Comment on "Create AdvancedBan on velocity." AdvancedBan Github Issue. https://github.com/DevLeoko/AdvancedBan/issues/547#issuecomment-1013552009 [↩](#note11ret)

### Disclaimer

Please note that no harm is meant to subjects of criticism. If the writing sounds harsh, we apologize; please let us know and we will make the language less harsh.
