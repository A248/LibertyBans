### Punishments

There are 4 types:
Ban, Mute, Kick, and Warn.

Each punishment may apply to a player or IP address. How IP address-based punishments are enforced depends on your configuration's address-strictness option.

### Commands

The basic format for /ban, /mute, and /warn is `/<command> <target> [time] <reason>`

When you ban/mute/warn a player, if you do not specify a time argument, the player will be banned permanently.
E.g.:
* `/ban ExamplePlayer Obedience is liberating` -> ExamplePlayer is banned permanently with reason "Obedience is liberating"
* `/ban ExamplePlayer permanently Obedience is liberating` -> ExamplePlayer is banned permanently with reason "Obedience is liberating"
* `/ban ExamplePlayer 5d Obedience is liberating` -> ExamplePlayer is banned for **five days** with reason "Obedience is liberating"

### Time Formatting
Time formatting follows a simple formula:
`<Number><TimeUnit>`

Available time units:
* m = minutes
* h = hours
* d = days
* mo = months

Examples:
* 3h -> 3 hours
* 8d -> 8 days
* 1mo -> 1 month
* 1m -> 1 minute

### Targets

It is possible to ban players using their name or IP address.

* If you specify a name, LibertyBans bans by UUID, so name changes do not allow players to become unbanned.
* If you specify an IP address, LibertyBans bans by IP address, which bans all players using that IP address (See also [Punishment Enforcement](Punishment-Enforcement_-Lenient,-Normal,-and-Strict-settings)).

Banning examples:

* `/ban DeviousPlayer` - bans DeviousPlayer.
* `/ban 192.110.162.103` - bans the IP address 192.110.162.103.
* `/ipban 192.110.162.103` bans the IP address 192.110.162.103. Same as `/ban 192.110.162.103`
* `/ipban DeviousPlayer` - bans the IP address of DeviousPlayer.

Unbanning examples:

* `/unban SaintlyPlayer` - Unbans SaintlyPlayer.
* `/unban 192.222.222.222` - Unbans the IP address 192.110.162.103.
* `/unbanip 192.222.222.222` Unbans the IP address 192.110.162.103. Same as `/unban 192.110.162.103`
* `/unbanip SaintlyPlayer` - Unbans the IP address of SaintlyPlayer.

