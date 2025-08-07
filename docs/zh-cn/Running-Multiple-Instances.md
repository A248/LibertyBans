
This is an advanced topic which most users will not need to consider.

### Motivation

There are some reasons you might want to run multiple instances of LibertyBans:
* You are running multiple proxies
* You are running a single proxy and want LibertyBans on the backend servers for a particular reason

### Multi-Instance Usage

You should place LibertyBans on the backend servers, and not the proxies.

LibertyBans is designed to synchronize punishments across instances, but only across multiple backend instances.

## Configuration

### Database

Configure all instances to connect to the same database in the `sql.yml`.

It is not possible to synchronize punishments using the default (local) database, HSQLDB. You need to use a remote database, such as MariaDB or PostgreSQL.

### Synchronization

You must also enable cross-instance synchronization in the `sql.yml`:
  * Set `synchronization.mode` to an available setting other than 'NONE'. For example:
```yaml
synchronization:
  mode: 'ANSI_SQL' # Synchronizes punishments between servers
```
  * More modes may be implemented upon feature request.

### Current Limitations

* If you kick a player who is offline, the punishment will go through. LibertyBans will allow you to "kick" offline players. The kick will be recorded in punishment history.

These limitations may be removed in the future, if they garner enough attention.
