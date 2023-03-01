
A composite punishment is one which applies to a UUID and an IP address. Composite punishments are an advanced topic, and you should be sure to understand how they work before you employ them.

### Terminology

This page will use the term "composite ban" for simplicity and convenience. However, it is also possible to create composite punishments which are mutes, warns, and kicks, in the same manner.

You should be familiar with UUIDs and IP addresses, the relation of these to users, and when these change.

## Definition

* A user ban applies to a UUID.
* An IP ban applies to an IP address.
* A composite ban applies to a UUID and an IP address.

### Features

With respect to enforcing punishments, a composite ban is much akin to an IP-ban.
* Using LENIENT strictness, any user with the banned UUID or the banned address will be banned.
  * Composite bans are thus useful if you desire punishment stricter than a LENIENT IP ban, which bans only players with the address, but not as restricting as a NORMAL IP ban, which bans all players that have ever had that address.
* Using NORMAL, STERN, or STRICT strictness, the applicable users are the same as if the ban was an IP ban.
  * This is because, under STERN/NORMAL/STRICT strictness, an IP ban will always apply to the user who had that IP address, so it will be as if the user's UUID was banned too.
  * Recall the explanations of [punishment enforcement](Punishment-Enforcement_-Lenient,-Normal,-and-Strict-settings)

With respect to creating, undoing, and listing punishments with commands, composite bans are arguably easier to use than IP bans.
  * In punishment messages, composite victims are displayed as a username rather than an IP address.
  * If you use the `composite-by-default` config option:
    * `/ban Player1` creates a composite ban.
    * `/unban Player1` undoes a composite ban.
  * Because a composite ban applies to a user, it will show up in /history, /warns, and other listing commands for that user. 

### Caveats

Normally, it is impossible to create a ban or mute applying to an existing banned or muted UUID or IP address. If you ban Player1, then attempt to re-ban Player1, LibertyBans will tell you this player is already banned.

* However, one *can* create different composite bans applying to the same UUID but different IP addresses.
* One can also create different composite bans applying to different UUIDs but the same IP address.

To clarify matters:
* You cannot create multiple bans or mutes applying to the exact same victim. A victim is a punishment subject such as:
  * A UUID, in the case of a UUID punishment
  * An IP address, in the case of an IP punishment
  * A UUID and IP address combination, in the case of a composite punishment
* Two separate composite punishments, applying to the same UUID but different IP addresses, have *different victims*, therefore it is possible for them to co-exist as active punishments.

### Enacting and Revoking Punishments

Most of the time, the composite victim functionality should simply function in a manner which is intuitive and friendly. However, this friendly top-level interface is achieved by somewhat complex mechanisms. It is best to document these here.

Enacting a punishment is simple. The target user's UUID and address combination is punished.

Revoking a punishment tries to be as helpful as possible in determining which punishment needs to be revoked.
* The first punishment which matches the specified criteria is revoked:
  * If you unban a username, the plugin attempts to unban the UUID. If that fails, the plugin attempts to undo any composite ban whose UUID is the specified user's UUID, disregarding the address of the undone composite ban.
  * If you unban an IP address, the plugin attempts to unban the IP address. If that fails, the plugin attempts to undo any composite ban whose IP address is the specified IP address, disregarding the UUID of the undone composite ban.
  * In other words, LibertyBans tries to find any punishment which you might mean when you type `/unban Player1' or `/unban 127.0.0.1` - either a simple victim or a composite victim.
  * Note that this process is performed in a single database query.
* This functionality happens regardless of whether you use the composite-by-default feature.

## Conclusion

Please be sure that you understand composite punishments before you employ them.
