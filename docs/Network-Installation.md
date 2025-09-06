
This page is an all-in-one guide for installing on proxies and networks. If you're not running Velocity or BungeeCord, this page is not for you.

### Basics

As a network administrator, you have many options for how to install proxy-wide software. A plugin like LibertyBans can run on the proxy itself, on the backend servers, or even both at the same time (although not recommended). This page will cover the advantages and disadvantages of installing in each location, the requirements, and the setup needed to ensure features work properly.

### Proxy Installation

Installing LibertyBans on a single proxy is the simplest route and will suit most users.

Steps:
1. Install a proxy-based permissions manager.
2. Install LibertyBans on the proxy.
3. Configure proxy permissions.
4. If you're running Velocity with 1.19.4+ or later, you need to install [SignedVelocity](https://modrinth.com/plugin/signedvelocity) on the proxy **and** backend servers.

Remember that proxy permissions are separate from backend server permissions. Sometimes permissions plugins have different commands for the proxy versus backend servers -- for example, LuckPerms uses `/lpb`  not `/lp` when it it running on BungeeCord. Lastly, being server OP is useless, because proxies do not have OPs.

### Backend Servers

Some people may want to install LibertyBans on multiple backend servers. There are a few reasons:
* Backend server plugins can't execute console commands. So if you want backend plugins (like anticheats) to execute punishment commands, you need LibertyBans installed on the backend servers.
* Backend server plugins can't use proxy APIs. So if you have backend server plugins that depend on the presence of LibertyBans (like discord webhooks, or punishment GUIs) you may need LibertyBans installed on the backend servers.
* You don't want to install SignedVelocity. You don't want to install LuckPerms-Velocity or LuckPerms-BungeeCord. You just want a simple, backend plugin and you don't want proxy plugins because they are a pain to update and require restarting the whole proxy.

In that case, please install LibertyBans on every single backend server. You can copy the configuration files from one instance to another. However, the most important thing is to **configure multi-instance synchronization**.

Please scroll down for how to do that.

### Partial Backend Coverage

A few users will seek to install LibertyBans on a limited set of backend servers. For example, let's say you want a "hub" server where banned players can join, but not the other servers like PvP or survival mode. Or, you're running a cracked network with an authentication server.

If this describes you, please switch **off** the option `platforms.game-servers.use-plugin-messaging` in your config.yml. By turning this feature off, banned players will not be kicked from the entire network, and they will be sent to the lobby instead (as handled by your proxy software).

### Cracked Networks

If you're running a cracked network -- with unauthenticated Minecraft accounts -- you need to be careful that LibertyBans can only "see" fully authenticated users.

By default, LibertyBans records the UUID and IP address of every user who logs in. If LibertyBans is on the proxy and unauthenticated players join the proxy, this can cause a problem leading to false IP bans.

To remedy this, you should use a limbo server. A limbo server is a specific backend server where unauthenticated users go, and they can't leave the limbo server unless logged in.

After setting up a limbo server, look at the `alts-registry` setting in the config.yml. There, you can ensure that players joining the limbo server won't have their IP addresses recorded. Steps:
* If running LibertyBans on the proxy:
  * Set `register-on-connection` to false.
  * Write the names of the limbo servers under `servers-without-ip-registration`.
* If running LibertyBans on the backend servers:
  * Set `register-on-connection` to false in the limbo servers' LibertyBans config.
  * Set `register-on-connection` to true everywhere else. 

Alternatively, you could use partial installation of LibertyBans (on some backend servers) to achieve the same effect:
  * Only install LibertyBans on the backend servers where fully authenticated players can join. That is, every backend server *except* the limbo server.
  * This will achieve the same goal of not logging false IP address history, and you won't need to touch the `alts-registry` settings.
  * Note that banned players will be able to join the limbo servers, but that may not matter to you.

### Multiple Proxies

If you're here, you know who you are. Please let us know how LibertyBans suits your needs, where it could use improvement, and if you found or suspect any performance bottlenecks.

## Multiple Instances

### Motivation and Requirements

We covered two reasons you might want to run multiple instances of LibertyBans:
* You are running multiple proxies
* You are running a single proxy and want LibertyBans on the backend servers

You will need:
* A remote database, like MySQL or MariaDB or PostgreSQL.

### Configuring the Database

Configure all instances to connect to the same database in the `sql.yml`.

It is not possible to synchronize punishments using the default (local) database, HSQLDB. So configure your database credentials on every instance -- everywhere LibertyBans is installed.

You must also enable cross-instance synchronization in the `sql.yml`. Set `synchronization.mode` to the value "ANSI_SQL", which will let LibertyBans use your database as a messenger.
```yaml
synchronization:
  mode: 'ANSI_SQL' # Synchronizes punishments between servers
```

We might implement more synchronization modes (like Redis, RabbitMQ, Kafka) if there is demand in the future.

### Final Notes and Limitations

**Kicking offline players**: LibertyBans can allow you to "kick" offline players if you are using synchronization. The kick will be recorded in punishment history.

**Installing on proxy and backends simultaneously**: Installing LibertyBans on the proxy and backends at the same time is heavily discouraged and officially unsupported, but we will make an effort to help you if there are problems with it. You will need to configure permissions on the proxy and backend servers to avoid receiving double notifications for new punishments.
