LibertyBans supports importing from the following sources:

* AdvancedBan
* LiteBans
* Vanilla server bans (includes Essentials)
  * Importing vanilla bans requires that you run LibertyBans on Bukkit during the import process. Once the import is complete you can move LibertyBans to any supported platform.
* LibertyBans itself, for the purpose of switching storage backends. See [self-importing](Self-Importing) for more information.

If you do not see your plugin listed here, open a [new issue](https://github.com/A248/LibertyBans/issues) and describe the feature request.

# Importing Steps

1. Backup your data. Taking backups is good practice, *always*. If you want to be extra safe, be sure that you can restore your backup, too.
2. Configure the `import.yml`
2. Run the import command - `/libertybans import <source>`. Replace <source> with 'advancedban', 'litebans', or 'vanilla'

# Caveats

Importing is not a 1-to-1 process, because storage methods vary across plugins.

## AdvancedBan and Vanilla – UUID Support

| Import source  | UUID Support - Victims | UUID Support - Operators |
| -------------- | ---------------------- | ------------------------ |
| LiteBans       | ✔️ | ✔️ |
| AdvancedBan    | ✔️ | ❌ |
| Vanilla        | ❌ | ❌ |



AdvancedBan and vanilla bans both use names in some places where LibertyBans needs UUIDs. LibertyBans uses UUIDs everywhere, so a UUID lookup will be performed in these cases.

How UUID lookups are performed is controlled by the uuid-resolution and server-type settings in the `config.yml`. 

* If you are running a normal online-mode server, you will want to add additional web API resolvers. Without this, your server will be rate-limited by Mojang for performing too many UUID lookups:
```
player-uuid-resolution:
  server-type: 'ONLINE'
  web-api-resolvers:
    - 'ASHCON' // Example - Add this first
    - 'MOJANG' 
```

* If you are running an offline-mode (cracked) server, and using `CRACKED` UUIDs – i.e., all players have cracked UUIDs – then you don't need to do anything. LibertyBans will assume the user is cracked and calculate their cracked UUID.

* If you are running an offline-mode server, and using `MIXED` UUIDs, where some players have cracked UUIDs and some have online UUIDs, LibertyBans cannot perform any UUID lookups. Some punishments *will be skipped* if LibertyBans cannot determine the UUID.
  * You can avoid losing punishments if all the users join the server first, or have joined the server since LibertyBans was installed. If the players have joined the server before, LibertyBans will remember their name and UUID, and so when the import occurs, LibertyBans will know the UUID of these players.

## LiteBans – Using  H2

If you used LiteBans with MariaDB, MySQL, or PostgreSQL, no additional setup is required. LibertyBans includes these drivers.

If you used LiteBans with H2, you will need to add the driver to your server first. How you do this is as follows:

1. Download the H2 driver:
  * [Direct download link](https://repo1.maven.org/maven2/com/h2database/h2/1.4.199/h2-1.4.199.jar)
  * [Maven Central link](https://mvnrepository.com/artifact/com.h2database/h2/1.4.199),
2. Create a directory called "libs" on your server. The "libs" directory should be located like this:
```
libs/
logs/
plugins/
server.properties
bukkit.yml
spigot.yml
paper.yml
server.jar
... etc
```
3. Upload the driver you downloaded in Step 1 to the libs directory
4. Now you will need to start your server so that it recognizes the libs directory. To do this, you need to use the `-classpath` option instead of `-jar`.
  * Start your server like this. Replace 'server.jar' with your server jar:
```
java -classpath server.jar:libs/* org.bukkit.craftbukkit.Main
```
  * Do NOT use `-jar`!
  * If you are running a proxy, you will need to replace `org.bukkit.craftbukkit.Main` with the main class of your proxy software. For Velocity, that is `com.velocitypowered.proxy.Velocity`. For BungeeCord, that is `net.md_5.bungee.Bootstrap`.
5. Run the import process as you normally would

For help, join the discord for support.