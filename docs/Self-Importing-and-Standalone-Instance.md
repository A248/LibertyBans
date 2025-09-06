
## Self-Importing

LibertyBans allows you to switch storage backends by importing its own data.

This is accomplished by simply copying all the data from the old database to the new database.

### Steps

1. Backup your data. Taking backups is good practice, *always*. If you want to be extra safe, be sure that you can restore your backup, too.
2. Configure the `import.yml` with the *old database* to import from.
3. Obtain a fresh, clean database -- the *new database* you want to use. Configure the `sql.yml` with this new database.
4. Restart LibertyBans with `/libertybans restart` and then run `/libertybans import self`.
5. Wait for the self-import process to complete. Don't join the server yet.

The new database **MUST** be empty before you run the self-import process. If the new database already has data in it, the migration is very likely to fail.
* **This means you cannot join the server until the self-import process is complete**, since joining the server will add data to the database.
* This also means you cannot create punishments on the new database before importing.

### Making it easier

If you don't want to fire up a full Minecraft server (maybe because it's slow), you can consider using the standalone version of LibertyBans. The standalone version can help you inspect the database, test out commands, and make sure the import happened correctly.

## Standalone

The standalone instance is a command-line version of LibertyBans.

### Starting

The LibertyBans jar can literally run by itself. You start it like any other Java program -- just like a Minecraft server -- using `java -jar LibertyBans.jar`.

This will launch a console, where you can type commands just like you would in a regular server console. The only difference is that every command is a LibertyBans command. So, you can type "ban A248" or "addon list" and it will behave exactly as you might expect.

### Connecting to an existing database

Please note that you cannot connect LibertyBans standalone to the same HSQLDB database as a running Minecraft server. Local databases like HSQLDB are not supposed to be shared between running JVM applications. Do not try this.

However, if you are using a remote database like MySQL or MariaDB, you can connect LibertyBans standalone to the same database. This will let you execute commands, list punishments, and modify player records. If you have multi-instane synchronization enabled, you can even use a standalone instance to execute commands that affect the other servers in your network, like kicking or banning other users.

### Stopping

Type 'stop' and press enter to shut down.
