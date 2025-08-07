
LibertyBans 1.1.0 will not break your setup and is fully compatible with 1.0.x. However, you may need to upgrade your technology or make minor configuration changes.

## Java Version Requirement

Java 17 is required for LibertyBans 1.1.0.

If you need assistance upgrading to Java 17, we are happy to help. Please ask, but be patient.

## For MariaDB Users

It is highly recommended to update to at least MariaDB 10.6. Although running MariaDB 10.3-10.5 is possible, support for these older versions is deprecated and may be removed in a later release.

Ultimately, you are responsible for updating your database.

Performance will be slightly and unavoidably degraded when running MariaDB 10.3, 10.4, or 10.5, because LibertyBans rewrites its SQL queries to achieve compatibility with legacy vendor-specific SQL syntax.

## For Floodgate Users

* LibertyBans now detects Geyser/Floodgate automatically. In most cases, you no longer need to manually configure the Geyser name prefix in LibertyBans' config.yml.
* There is one exception to automatic Geyser/Floodgate detection. If you installed LibertyBans in a different place -- for example, LibertyBans is on the proxy but Floodgate is on the backend servers -- you will need to manually configure LibertyBans to use a specified prefix. This uses a new configuration option called `force-geyser-prefix`.

The `GEYSER` server-type has been removed. **You will need to update the config.yml with an appropriate `server-type`:** ONLINE, OFFLINE, or MIXED.
