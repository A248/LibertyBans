# LibertyBans
The be-all, end-all of discipline.

## Introduction

### Features

* Option to use MySQL. Local file database (using HSQLDB) is available for those who do not need MySQL.
* No complicated installation. All dependencies are automatically downloaded with hard-coded secure SHA-512 hashes used to validate the downloads.
* Enhanced database-oriented performance emphasising database-side calculations through stored procedures. Further optimisation on MySQL using MySQL events.
* Compact database storage setup. UUIDs are stored in minimal form as raw bytes instead of strings. Same for IP addresses.
* Best practices for asynchronous calculations are followed. The performance cost of context switching is understood and avoided; the plugin does not blindly fire async tasks.
* Designed for high availability and concurrency. Minimal locking is employed while keeping state consistent; this is mostly realised through the fact that most plugin state is maintained in the database itself.
* Well-structured API providing a framework for other plugins to work with the plugin.

### Why?

I noticed the striking predominance of closed-source, premium punishment plugins, particularly one which I won't name (it will be obvious for many readers). I did not like this, so I decided I would set out to change it by making a free, higher-quality alternative.

Yes, that's right. This plugin strives to be better at a lower price.

## Basic Info

### Requirements

* Java 11 (or higher)
* A compatible server platform

### Supported Platforms

* Spigot/Paper or any forks thereof
* BungeeCord
* Velocity

### Why Java 11?

There have been several major improvements to Java for developers and server owners. Newer Java versions are faster for server owners and easier to develop with for developers. I would strongly recommend updating to Java 11 or 14.

Here is just a snapshot of the advantages:

* Jigsaw modularisation
* CompletableFuture enhancements
* StackWalker API
* Low-level concurrency APIs as safe and supported alternatives to sun.misc.Unsafe
* Ahead of time compilation
* Prebuild minimized runtime images
* Compact strings
* IO-related constructors accept Charset instead of a String.
* try-with-resources improvements
* File IO additions
* var keyword
* HttpClient API
* Immutable collection factory methods
* Additions to streams
* Experimental JIT compiler from GraalVM
* Improvements to the G1 garbage collector, as well as more modern, state-of-the-art garbage collectors (ZGC and Shenandoah)
* TLS 1.3 support

Of the many performance improvements, hastening of reflection, more compact Strings, and improved hash structure performance are especially relevant for Minecraft servers.

The many new tools available to developers further allow them to create faster, higher-quality programs with less code.

## Other Information

I am also a contributor to AdvancedBan. If you absolutely need Java 8 support I would recommend you use it. AdvancedBan has some problems with its design, and although I helped to improve it, there are some choices I needed to make differently in this plugin.

### License

LibertyBans is licensed under the GNU AGPL v3. See the license file for more information.
