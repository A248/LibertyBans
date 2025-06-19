Normally, when you punish or unpunish a player, two messages are sent:

1. A message to the player running the command, telling them it was successful
2. A message to all other players with a certain permission, telling them a player was punished or unpunished.

This second message is called the notification message. It is usually sent to staff members.

## Silent Messages

The `-s` option allows punishing or unpunishing a player silently. A silent message does not show the notification message.

For example:
* `ban -s A248 You are banned`
* `ban A248 30d -s Banned for 30 days`
* `unban A248 -s`

As shown in these examples, `-s` can come anywhere before the punishment reason.

## Controlling the Silent Feature

* Only players with the permission `libertybans.<type>.silent` or `libertybans.<type>.silentundo` can use the silent feature.
* Staff members with the permission `libertybans.<type>.notifysilent` or `libertybans.<type>.unnotifysilent` will always see notification messages, regardless of whether `-s` was used.
where `<type>` is the punishment type like 'ban' or 'mute'

See the [Permissions](Permissions) page for more information.
