Here's how punishments are enforced. I'll use bans in this explanation, but the same can be said for mutes.

## Normal Ban
The ban applies only to the target user. That user may change their name, but they will still be banned according to their UUID.

## IP Bans

How an IP ban is applied depends on the `address-strictness` you configure.

### Lenient strictness

This is the same behavior as most punishment plugins. It is also the most intuitive.

When using lenient strictness, IP bans apply to:
 * Users joining with the banned IP address

### Normal strictness

This is the default option. It is designed to eliminate loopholes (e.g., player changes their IP address) and limit the usage of alt accounts (using another account to bypass a ban). This mechanism is effective enough to eliminate the need for an alt-check plugin.

When using normal strictness, IP bans apply to:
 * Users joining with the banned IP address
 * Users who have previously joined with the banned IP address

### Strict strictness

The strict option is the most effective. Arguably it is too strict, which is why it is not the default.

When using strict strictness, IP bans apply to:
 * Users joining with the banned IP address
 * Users who have previously joined with the banned IP address
 * Users who have previously joined with an IP address which the user who is banned has also joined with.