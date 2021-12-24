
This is an advanced topic which most users will not need to consider.

### Motivation

There are some reasons you might want to run multiple instances of LibertyBans:
* You are running multiple proxies
* You are running a single proxy and want LibertyBans on the backend servers for a particular reason

### Multi-Proxy Usage

If you are using LibertyBans in a multi-proxy setup, you should place it on the backend servers, and not the proxies.

LibertyBans is designed to work on multi-proxy setups insofar as it is placed on the backend servers.

### Configuration

You must enable cross-instance synchronization in the sql.yml:
  * Set `synchronization.mode` to an available setting other than 'NONE'. For example:
```yaml
synchronization:
  mode: 'ANSI_SQL' # Synchronizes punishments between servers
```
  * More modes may be implemented upon feature request.
  
### Current Limitations

* If you kick a player who is offline, the punishment will go through. LibertyBans will allow you to "kick" offline players. The kick will be recorded in punishment history.

These limitations may be removed in the future, if they garner enough attention.
