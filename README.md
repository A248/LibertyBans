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

### Why?

I noticed the striking predominance of closed-source, premium punishment plugins, particularly one which I won't name (it will be obvious for many readers). I did not
like this, so I decided I would set out to change it by making a free, higher-quality alternative.

Yes, that's right. This plugin strives to be better at a lower price.

## Basic Info

### Requirements

* Java 11
* A compatible server platform

### Supported Platforms

* Spigot/Paper or any forks thereof
* BungeeCord
* Velocity

### Other Information

I am also a contributor to AdvancedBan. If you absolutely need Java 8 support I would recommend you use it. AdvancedBan has some problems with its design, and although
I helped to improve it, there are some choices I needed to make differently in this plugin.
