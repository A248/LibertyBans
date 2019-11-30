## ArimBans.sk and ArimBans2.sk ##

### What is a script? ###

In this context, a script is a runnable **Skript** file, ending in .sk. Skript is a "coding language" which allows one to easily modify server features.
Skript contains simple syntaxes for new commands, listening to events, setting variables. It's nearly English.
I highly recommend Skript for those with no programming experience whatsoever. Everything related to Skript may be found [here](https://skunity.com).

### Why are these scripts here? ###

I thought it necessary to include them in this repo, as they are the beginnings of the ArimBans project.
The scripts `-ArimBans.sk` and `ArimBans2.sk` correspond to ArimBans1.0 and ArimBans2.0.

Though Skript is very useful for boilerplate operations, it simply does not provide the full range of tools available in pure Java.
Especially, multi-threading and custom data storage are lacking in Skript and its array of addons.
Most importantly, Skript is limited by its syntaxes provided by itself and addons. Accordingly, I sought to create
**ArimBans3** on 20 October 2019, when its first source files were generated.

### Note well ###

The files listed under `scripts/` are relics of the past. Neither are supported.
* ArimBans.sk is *incomplete*, so it is disabled by prepending "-" (any script can be disabled this way).
* ArimBans2.sk is completed and functional. ArimBans2.sk was originally designed for 1.8.8 servers using Skript2.2-dev25.
It is not guaranteed to work on newer Skript versions. Due to a bug in Skript2.2-dev25, you may occassionally receive unix to date parse errors.
