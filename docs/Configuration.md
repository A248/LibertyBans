
The configuration is mostly self-explanatory.

### Files

Open up the `plugins/LibertyBans` folder. You can find all the configuration files there.

* `config.yml` - The main configuration file. Most behavior-related settings are here.
* `lang/` - The directory containing the languages/messages configuration.
  * The file `messages_en.yml` (English) is the default.
  * Each language file is named after the language supported, e.g. `messages_fr.yml` for French or `messages_ru.yml` for Russian.
  * You can switch the plugin language by looking in `config.yml` and do CTRL+F for "language."
* `sql.yml` - If you want to change database settings, go here.

### Syntax

LibertyBans uses familiar ".yml" files. Feel free to use [Online Yaml Parser](https://yaml-online-parser.appspot.com) to validate config files.

## Reloading Configuration

### Reload command

If you have the necessary [permissions](Permissions), you can reload most of the configuration with the reload command.

```
/libertybans reload
```

This operation is fully "atomic" with respect to server processes. What that means is you can run this command while players are online and your server is booming -- with absolutely no consequences. The configuration and messages will be updated in-place and your server will keep running as usual.

### Restarting the plugin

For settings in sql.yml, and some other settings where documented, you need to either fully restart your server, or use LibertyBans' restart command:
```
/libertybans restart
```

This is a heavy restart that re-connects to the database, re-registers commands, and everything else. Yes, you can run it on a live server and nothing will happen, but there is a small window of time (1-2 seconds) during which a banned player could join.

### Addons

Installing [addons](Addons) requires a full server restart.
