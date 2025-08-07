
This page explains how punishments are enforced. I'll use bans as an example, but the same can be said for mutes.

Punishment enforcement depends on the `address-strictness` setting you configure.

## Choosing an Address Strictness

The default address strictness is `NORMAL`. The behavior in other punishment plugins is similar to `LENIENT` in LibertyBans.

For large servers, there are also [performance considerations](Database-Performance) to choosing the strictness.

## User Ban

On lenient, normal, or stern address strictness:
* The ban applies only to the target user. That user may change their name, but they will still be banned according to their UUID.

On strict strictness, user bans are treated as if IP bans.

## IP Bans

Behavior depends on the strictness configured.

### Lenient strictness

This is the same behavior as most punishment plugins. It is also the most intuitive.

When using "lenient" strictness, IP bans apply to:
 * Users joining with the banned IP address

### Normal strictness

This is the default option. It is designed to eliminate loopholes (e.g., player changes their IP address) and limit the usage of alt accounts (using another account to bypass a ban). This mechanism is effective enough to eliminate the need for an alt-check plugin.

When using "normal" strictness, IP bans apply to:
 * Users joining with the banned IP address
 * Users who have previously joined with the banned IP address

### Stern strictness

The "stern" option is the most effective. Arguably it is too severe, which is why it is not the default.

When using "stern" strictness, IP bans apply to:
 * Users joining with the banned IP address
 * Users who have previously joined with the banned IP address
 * Users who have previously joined with an IP address which the user who is banned has also joined with

### Strict strictness

This setting includes the additional stipulation that user bans are enforced as stringently as IP-bans.

When using "strict" strictness, **user and IP bans** apply to:
* Users joining with the banned UUID or IP address
* Users who have previously joined with the banned IP address
* Users who have previously joined with an IP address which the user who is banned has also joined with
