
LibertyBans' performance is strongly related to database query performance. Fast database access equates to fast plugin performance.

This page is mostly useful for large servers looking to ensure efficient operation.

### Performance Factors

Performance largely depends on the database queries issued to check incoming players for bans and chatting players for mutes. Every time a player connects to the server, or sends a chat message, a database query is executed. If these queries take longer, connections and chat messages will become delayed.

To reduce database queries issued for chat messages, LibertyBans uses a mute cache. This prevents players chatting frequently from overwhelming the database. The mute cache keeps track of players who recently chatted and whether they are muted.

## Performance Configuration

### Address Strictness

The configured address strictness directly affects query performance. As players log in or start chatting, queries are run to check if they are banned or muted.

Scalability is the primary concern. If a query is scalable, its execution time will not depend on the quantity of data. A scalable query should yield similar results on a database with 100 punishments as on a database with 100,000 punishments.

As a short guide:
1. `LENIENT` and `NORMAL` scale well on all databases. It is recommended to use one of them.
2. `STERN` is mostly scalable. The query is rather complicated, so it's worth running
3. `STRICT` is scalable on MariaDB (and probably MySQL and PostgreSQL), but **not** the default HSQLDB database as investigated in [#190](https://github.com/A248/LibertyBans/issues/190).

Large servers are heavily discouraged from using a strictness that does not scale with their database (for example, HSQLDB with the `STRICT` setting).

Checking whether a SQL query is scalable involves running `EXPLAIN ...` in the SQL database. We did this for several databases usable with LibertyBans, and here are the full results:

### Connection Pool Size

Following recommended technical practice, LibertyBans uses a fixed connection pool. By default, the connection pool is quite small with only 6 connections.

You will likely be surprised to find that a pool of 6 connections can service a large server with many players connecting and chatting. For a large network with LibertyBans installed on the proxy, you may wish to increase the connection pool size. For a large network with many backend servers, but a low or medium player count (<20) on each backend server, you may find that a smaller connection pool conserves resources better.

Please keep in mind that a larger connection pool does not necessarily mean better performance. Too often, administrators make pool sizes too large, which in fact reduces performance. Read [this page](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing) to better understand connection pool sizing.

### Mute Cache Settings

Increasing the mute cache expiration time means mutes will be stored for longer, and therefore, fewer queries will be made to the database.

The expiration semantic does affect performance, but it should be configured on the basis of correct behavior as priority.

## Common Issues

### Network Latency

If your database is slow to respond, that will translate to LibertyBans taking longer to check bans and mutes.

Ensure the database is not located in a geographically distant region (for example, a MySQL database in Antarctica).

### Connection Pool Overload

If you run a large network with LibertyBans installed on the proxy, the default connection pool size may be insufficient to meet your needs. If you repeatedly notice players taking too long to login or chat, try increasing the connection pool size.

If you are familiar debugging, it is fairly easy to investigate connection pool load by using either live debugging tools or logging configuration to monitor connection pool events.

Another solution to an overloaded connection pool is more aggressive mute caching.

## Appendix

*Applicability query, NORMAL, default settings*

```sql
```

*Applicability query, STERN, default settings*

```sql
select "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from (
  select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
    where ("libertybans_strict_links"."uuid2" = cast('51ac4257-ea95-4b32-ab90-2e79fdae4b21' as uuid) and "libertybans_applicable_bans"."victim_type" <> 0)
  union all select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    where "libertybans_applicable_bans"."uuid" = cast('51ac4257-ea95-4b32-ab90-2e79fdae4b21' as uuid)
) as "sq_union"
where ("end" = 0 or "end" > 1621440000) order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1
```

*Applicability query, STERN, cracked network alt registry settings*

```sql
select "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from (
  select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
    where ("libertybans_strict_links"."uuid2" = cast('51ac4257-ea95-4b32-ab90-2e79fdae4b21' as uuid) and "libertybans_applicable_bans"."victim_type" <> 0)
  union all select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    where ("libertybans_applicable_bans"."uuid" = cast('51ac4257-ea95-4b32-ab90-2e79fdae4b21' as uuid) or ("libertybans_applicable_bans"."victim_type" <> 0 and "libertybans_applicable_bans"."address" = X'1EF97F89'))
) as "sp_union"
where ("end" = 0 or "end" > 1621440000) order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1
```

*Applicability query, STRICT, default settings*

```sql
select "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_applicable_bans"
  join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
where (("end" = 0 or "end" > 1621440000) and "libertybans_strict_links"."uuid2" = cast('d0eda4ff-c40e-43ed-8765-794640deb8b3' as uuid))
order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1
```

*Applicability query, STRICT, cracked network alt registry settings*

```sql
select "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from (
  select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
    where "libertybans_strict_links"."uuid2" = cast('02e6b74a-a205-4a9e-96f4-f7e3491bd8bf' as uuid)
  union all select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    where ("libertybans_applicable_bans"."uuid" = cast('02e6b74a-a205-4a9e-96f4-f7e3491bd8bf' as uuid)
    or "libertybans_applicable_bans"."address" = X'C07D66F4')
) as "sp_union"
where ("end" = 0 or "end" > 1621440000)
order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1
```
