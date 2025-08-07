
LibertyBans offers a variety of tools to prevent alt accounts from circumventing punishment.

Some of them are automatic; some require manual action by staff members. They differ in sophistication and complexity. Some measures may result in blocking legitimate players. Therefore, it is important to understand the tools at your disposal and the implications of their use.

## IP-based punishments

A punishment can apply to an IP address. However, LibertyBans takes this feature further than competing punishment plugins by considering past IP addresses and discovering the network of associated alt accounts.

See [Punishment Enforcement](Punishment-Enforcement_-Lenient,-Normal,-and-Strict-settings) for more information.

## Composite punishments

A composite punishment applies to a UUID and an IP address. Rather than create two separate punishments, a composite punishment is a "two-in-one" punishment.

The co-location of UUID and IP address also makes composite punishments easier to track on a per-user basis.
* If you typically punishment IP addresses by default, consider using composite punishments instead.
* If you want to treat user punishments like IP-based punishments, consider composite punishments.

The idea is simple and effective but there are caveats. See [Guide to Composite Punishments](Guide-to-Composite-Punishments).

## Alt Checks

LibertyBans can perform manual alt-checks when requested by staff members, via the `/libertybans alts` command.

Additionally, an alt-check can be run automatically when a player joins. The "auto-show" feature does this -- every time a player joins, LibertyBans will run an alt check on them:
  * This feature is fully configurable.
  * For example, it is possible to notify staff members when the alt account of a banned player joins. This feature is fully configurable.

## Connection Limiter

There is a very basic connection limiter. If enabled, too many players joining on the same IP address within a certain time period will trigger the limit.

For example, if more than 5 players join from the same IP address within the past 10 minutes, no more joins will be allowed from that IP address.

This can help with bot attacks although is not a complete solution.
