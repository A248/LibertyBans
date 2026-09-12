
LibertyBans offers a variety of tools to prevent alt accounts from circumventing punishment.

Some of them are automatic; some require manual action by staff members. They differ in sophistication and complexity. Some measures may result in blocking legitimate players. Therefore, it is important to understand the tools at your disposal and the implications of their use.

# Punishing IP addresses

There are two ways to directly punish IP addresses:
* IP punishments
* Composite punishments (IP address + UUID)

## IP punishments

A punishment can apply to an IP address. 

However, LibertyBans takes this feature further than competing punishment plugins by considering past IP addresses. LibertyBans can discover the network of associated alt accounts that have used the same IP address, then ban all of them.

How strict LibertyBans considers past alt accounts is configurable. This is called "address strictness." See [this page](Punishment-Enforcement_-Lenient,-Normal,-and-Strict-settings) for more information on address strictness.

## Composite punishments

A composite punishment applies to a UUID and an IP address.

Like an IP punishment, composite punishments can be more or less strict depending on [address strictness](Punishment-Enforcement_-Lenient,-Normal,-and-Strict-settings). However, unlike a regular IP punishment, a composite punishment is a "two-in-one" punishment. It applies to a UUID and an IP address, in the same punishment.

The pairing of UUID and IP address can make composite punishments easier to track on a per-user basis. Reasons to use composite punishments include:
* You typically punish IP addresses by default, but want a nicer interface and punishments tied to users.
* You want to treat user punishments like IP-based punishments.

The idea is effective, but there are caveats. See [Guide to Composite Punishments](Guide-to-Composite-Punishments) for more information.

# Other enforcement options

## Alt checks

LibertyBans can perform manual alt-checks when requested by staff members, via the `/libertybans alts` command.

Additionally, an alt-check can be run automatically when a player joins. The "auto-show" feature does this -- every time a player joins, LibertyBans will run an alt check on them:
  * This feature is fully configurable.
  * For example, it is possible to notify staff members when the alt account of a banned player joins. This feature is fully configurable.

## Connection limiter

There is a very basic connection limiter. If enabled, too many players joining on the same IP address within a certain time period will trigger the limit.

For example, if more than 5 players join from the same IP address within the past 10 minutes, no more joins will be allowed from that IP address.

This can help with bot attacks although is not a complete solution.

# IP whitelisting

Sometimes, the enforcement options are too strict and end up blocking a staff member or an innocent player.

In these cases, it may be helpful to whitelist IP addresses. IP whitelisting exempts addresses on the whitelist from the following checks:
* IP punishments
* Composite punishments
* Automatic alt checks (manual `/alts` still works)
* Connection limiter

This feature should be used rarely, and it is not made for industrial scale. The IP addresses are stored in-memory due to performance/architectural limitations.
