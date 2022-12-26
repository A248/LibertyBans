
LibertyBans 1.1.0 will not break your setup and is fully compatible with 1.0.x. However, you may need to upgrade your technology or make minor configuration changes.

## Java Version Requirement

Java 17 is required for LibertyBans 1.1.0.

If you need assistance upgrading to Java 17, we are happy to help. Please ask, but be patient.

## For MariaDB Users

LibertyBans now requires at least MariaDB 10.6. You are responsible for updating your database server.

## For Floodgate Users

LibertyBans now detects Geyser/Floodgate automatically. You no longer need to manually configure the Geyser name prefix in LibertyBans' config.yml.

The `GEYSER` server-type has been removed. **You will need to update the config.yml with an appropriate `server-type`:** ONLINE, OFFLINE, or MIXED.
