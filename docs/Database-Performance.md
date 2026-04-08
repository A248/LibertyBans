
LibertyBans' performance is strongly related to database query performance. Fast database access equates to fast plugin performance.

This page is mostly useful for large servers looking to ensure efficient operation.

## Performance Factors

Performance largely depends on the database queries issued to check incoming players for bans and chatting players for mutes. Every time a player connects to the server, or sends a chat message, a database query is executed. If these queries take longer, connections and chat messages will become delayed.

To reduce database queries issued for chat messages, LibertyBans uses a mute cache. This prevents players chatting frequently from overwhelming the database. The mute cache keeps track of players who recently chatted and whether they are muted.

# Performance Configuration

## Punishment queries

The configured address strictness directly affects query performance. As players log in or start chatting, queries are run to check if they are banned or muted.

Scalability is the primary concern. If a query is scalable, its execution time will not depend on the quantity of data. A scalable query should yield similar results on a database with 100 punishments as on a database with 100,000 punishments.

### Log-ins and mute checks

Log-ins and mute checks are the most frequently issued queries, so this is the most important section.

For logins and mute-checking:
1. `LENIENT` and `NORMAL` scale well on all databases and configurations.
2. `STERN` is scalable, but weakly scalable on cracked networks with alts registry settings.
3. `STRICT` is weakly scalable.

Cracked networks refers to installing LibertyBans on the proxy and configuring alts registry settings.

Large servers are discouraged from using a strictness that does not scale with their database (for example, HSQLDB with the `STRICT` setting).

### Other plugins using the API

Note that while the queries inside LibertyBans are fine-tuned where possible, other plugins can also cause LibertyBans to execute queries through API calls. The data requested is arbitrary and in some cases, the resulting queries may not scale well.

An example would be requesting all of a player's *active* punishments through the API. The same problem does not exist with history, and this is a quirk of LibertyBans V1's design.

### Results table

Checking whether a SQL query is scalable involves running `EXPLAIN ...` in the SQL database. We did this for several databases usable with LibertyBans, and here are the full results:

Note that /history only matters when `show-applicable-for-history: true` is enabled in the config. If not using this option, you can disregard these rows.

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

**Applicability query, LENIENT, login or mute check (including cracked network)**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_simple_bans"
where (
  ("end" = 0 or "end" > 1621440000)
  and (
    ("libertybans_simple_bans"."victim_type" = 0 and "libertybans_simple_bans"."victim_uuid" = X'c3e224f08860473abcd377ed3961b963')
    or ("libertybans_simple_bans"."victim_type" = 1 and "libertybans_simple_bans"."victim_address" = X'FAFFD0042DD7473796DA9BF4B82BB962')
    or (
      "libertybans_simple_bans"."victim_type" = 2
      and ("libertybans_simple_bans"."victim_uuid" = X'c3e224f08860473abcd377ed3961b963'
        or "libertybans_simple_bans"."victim_address" = X'FAFFD0042DD7473796DA9BF4B82BB962'
      )
    )
  )
) order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;
```

**Applicability query, LENIENT, all of player's active punishments**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_simple_active"
where (("libertybans_simple_active"."victim_type" = 0 and "libertybans_simple_active"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a')
  or ("libertybans_simple_active"."victim_type" = 1 and "libertybans_simple_active"."victim_address" = X'6A087183')
  or ("libertybans_simple_active"."victim_type" = 2 and ("libertybans_simple_active"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a'
    or "libertybans_simple_active"."victim_address" = X'6A087183'
  ))
) order by "start" asc, "id" asc;
```

**Applicability query, LENIENT, player's history**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_simple_history"
where (("libertybans_simple_history"."victim_type" = 0 and "libertybans_simple_history"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a')
  or ("libertybans_simple_history"."victim_type" = 1 and "libertybans_simple_history"."victim_address" = X'6A087183')
  or ("libertybans_simple_history"."victim_type" = 2 and ("libertybans_simple_history"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a'
    or "libertybans_simple_history"."victim_address" = X'6A087183'
  ))
) order by "start" asc, "id" asc;
```

**Applicability query, NORMAL, login or mute check**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from "libertybans_applicable_bans"
where (
  ("end" = 0 or "end" > 1621440000)
  and "libertybans_applicable_bans"."uuid" = X'd46b4b373e584adf8510a691bbbc6b10'
) order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;
```

**Applicability query, NORMAL, login to cracked network**

```sql
explain select "libertybans_addresses"."address" from "libertybans_addresses"
where "libertybans_addresses"."uuid" = X'03a27a2a7f5c4bcbb369422de19f741f';

explain select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from "libertybans_simple_bans"
where (("end" = 0 or "end" > 1621440000)
  and (("libertybans_simple_bans"."victim_type" = 0 and "libertybans_simple_bans"."victim_uuid" = X'c3e224f08860473abcd377ed3961b963')
    or ("libertybans_simple_bans"."victim_type" = 1 and "libertybans_simple_bans"."victim_address" in (
      X'A32B077B',
      X'1EF97F89',
      X'C07D66F4',
      X'88D97498C221FD69BC893E0E6F37EC1A',
      X'A689353E',
      X'442B5E15AC6206689353E0EC96A8CB67',
      X'6AFFD004'
    ))
    or ("libertybans_simple_bans"."victim_type" = 2 and ("libertybans_simple_bans"."victim_uuid" = X'c3e224f08860473abcd377ed3961b963'
    or "libertybans_simple_bans"."victim_address" in (
      X'A32B077B',
      X'1EF97F89',
      X'C07D66F4',
      X'88D97498C221FD69BC893E0E6F37EC1A',
      X'A689353E',
      X'442B5E15AC6206689353E0EC96A8CB67',
      X'6AFFD004'
    )
  ))
)) order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;
```

**Applicability query, NORMAL, all of player's active punishments**

```sql
explain select
  "inner_victim_type" as "victim_type", "inner_victim_uuid" as "victim_uuid", "inner_victim_address" as "victim_address",
  "inner_type" as "type", "inner_operator" as "operator", "inner_reason" as "reason", "inner_scope_type" as "scope_type",
  "inner_scope" as "scope", "inner_start" as "start", "inner_end" as "end", "inner_track" as "track", "id"
from (
  select
    max("victim_type") as "inner_victim_type", max("victim_uuid") as "inner_victim_uuid", max("victim_address") as "inner_victim_address",
    max("type") as "inner_type", max("operator") as "inner_operator", max("reason") as "inner_reason", max("scope_type") as "inner_scope_type",
    max("scope") as "inner_scope", max("start") as "inner_start", max("end") as "inner_end", max("track") as "inner_track", "id"
  from "libertybans_applicable_active"
  where "libertybans_applicable_active"."uuid" = X'd46b4b373e584adf8510a691bbbc6b10'
  group by "id"
 ) as "sq_agg" order by "inner_start" asc, "id" asc;
```

**Applicability query, NORMAL, player's history**

```sql
explain select
  "inner_victim_type" as "victim_type", "inner_victim_uuid" as "victim_uuid", "inner_victim_address" as "victim_address",
  "inner_type" as "type", "inner_operator" as "operator", "inner_reason" as "reason", "inner_scope_type" as "scope_type",
  "inner_scope" as "scope", "inner_start" as "start", "inner_end" as "end", "inner_track" as "track", "id"
from (
  select
    max("victim_type") as "inner_victim_type", max("victim_uuid") as "inner_victim_uuid", max("victim_address") as "inner_victim_address",
    max("type") as "inner_type", max("operator") as "inner_operator", max("reason") as "inner_reason", max("scope_type") as "inner_scope_type",
    max("scope") as "inner_scope", max("start") as "inner_start", max("end") as "inner_end", max("track") as "inner_track", "id"
  from "libertybans_applicable_history"
  where "libertybans_applicable_history"."uuid" = X'd46b4b373e584adf8510a691bbbc6b10'
  group by "id"
 ) as "sq_agg" order by "inner_start" asc, "id" asc;
```

**Applicability query, STERN, login or mute check**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from (
  select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
    where ("libertybans_strict_links"."uuid2" = X'51ac4257ea954b32ab902e79fdae4b21' and "libertybans_applicable_bans"."victim_type" <> 0)
  union all select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    where "libertybans_applicable_bans"."uuid" = X'51ac4257ea954b32ab902e79fdae4b21'
) as "sq_union"
where ("end" = 0 or "end" > 1621440000)
order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;
```

**Applicability query, STERN, login to cracked network**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from (
  select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
    where ("libertybans_strict_links"."uuid2" = X'51ac4257ea954b32ab902e79fdae4b21' and "libertybans_applicable_bans"."victim_type" <> 0)
  union all
  select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    where ("libertybans_applicable_bans"."uuid" = X'51ac4257ea954b32ab902e79fdae4b21'
      or ("libertybans_applicable_bans"."victim_type" <> 0 and "libertybans_applicable_bans"."address" = X'1EF97F89')
    )
) as "sp_union"
where ("end" = 0 or "end" > 1621440000)
order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;
```

**Applicability query, STERN, all of player's active punishments**

```sql
explain select
  "inner_victim_type" as "victim_type", "inner_victim_uuid" as "victim_uuid", "inner_victim_address" as "victim_address",
  "inner_type" as "type", "inner_operator" as "operator", "inner_reason" as "reason", "inner_scope_type" as "scope_type",
  "inner_scope" as "scope", "inner_start" as "start", "inner_end" as "end", "inner_track" as "track", "id"
from (
  select
    max("victim_type") as "inner_victim_type", max("victim_uuid") as "inner_victim_uuid", max("victim_address") as "inner_victim_address",
    max("type") as "inner_type", max("operator") as "inner_operator", max("reason") as "inner_reason", max("scope_type") as "inner_scope_type",
    max("scope") as "inner_scope", max("start") as "inner_start", max("end") as "inner_end", max("track") as "inner_track", "id"
  from (
    select
      "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
    from "libertybans_applicable_active"
      join "libertybans_strict_links" on "libertybans_applicable_active"."uuid" = "libertybans_strict_links"."uuid1"
      where ("libertybans_strict_links"."uuid2" = X'65e16e917b7e4df692eb3172eb138235' and "libertybans_applicable_active"."victim_type" <> 0)
    union all
    select
      "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
    from "libertybans_applicable_active"
      where "libertybans_applicable_active"."uuid" = X'65e16e917b7e4df692eb3172eb138235'
  ) as "sq_union" group by "id"
) as "sq_agg" order by "inner_start" asc, "id" asc
```

**Applicability query, STERN, player's history**

```sql
explain select
  "inner_victim_type" as "victim_type", "inner_victim_uuid" as "victim_uuid", "inner_victim_address" as "victim_address",
  "inner_type" as "type", "inner_operator" as "operator", "inner_reason" as "reason", "inner_scope_type" as "scope_type",
  "inner_scope" as "scope", "inner_start" as "start", "inner_end" as "end", "inner_track" as "track", "id"
from (
  select
    max("victim_type") as "inner_victim_type", max("victim_uuid") as "inner_victim_uuid", max("victim_address") as "inner_victim_address",
    max("type") as "inner_type", max("operator") as "inner_operator", max("reason") as "inner_reason", max("scope_type") as "inner_scope_type",
    max("scope") as "inner_scope", max("start") as "inner_start", max("end") as "inner_end", max("track") as "inner_track", "id"
  from (
    select
      "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
    from "libertybans_applicable_history"
      join "libertybans_strict_links" on "libertybans_applicable_history"."uuid" = "libertybans_strict_links"."uuid1"
      where ("libertybans_strict_links"."uuid2" = X'65e16e917b7e4df692eb3172eb138235' and "libertybans_applicable_history"."victim_type" <> 0)
    union all
    select
      "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
    from "libertybans_applicable_history"
      where "libertybans_applicable_history"."uuid" = X'65e16e917b7e4df692eb3172eb138235'
  ) as "sq_union" group by "id"
) as "sq_agg" order by "inner_start" asc, "id" asc
```

**Applicability query, STRICT, login or mute check**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_applicable_bans"
  join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
where (("end" = 0 or "end" > 1621440000) and "libertybans_strict_links"."uuid2" = X'd0eda4ffc40e43ed8765794640deb8b3')
order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;
```

**Applicability query, STRICT, login to cracked network**

```sql
explain select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from (
  select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
    where "libertybans_strict_links"."uuid2" = X'02e6b74aa2054a9e96f4f7e3491bd8bf'
  union all select "libertybans_applicable_bans".* from "libertybans_applicable_bans"
    where ("libertybans_applicable_bans"."uuid" = X'02e6b74aa2054a9e96f4f7e3491bd8bf'
    or "libertybans_applicable_bans"."address" = X'C07D66F4')
) as "sp_union"
where ("end" = 0 or "end" > 1621440000)
order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;
```

**Applicability query, STRICT, all of player's active punishments**

```sql
explain select
  "inner_victim_type" as "victim_type", "inner_victim_uuid" as "victim_uuid", "inner_victim_address" as "victim_address",
  "inner_type" as "type", "inner_operator" as "operator", "inner_reason" as "reason", "inner_scope_type" as "scope_type",
  "inner_scope" as "scope", "inner_start" as "start", "inner_end" as "end", "inner_track" as "track", "id"
from (
  select
    max("victim_type") as "inner_victim_type", max("victim_uuid") as "inner_victim_uuid", max("victim_address") as "inner_victim_address",
    max("libertybans_applicable_active"."type") as "inner_type", max("libertybans_applicable_active"."operator") as "inner_operator",
    max("libertybans_applicable_active"."reason") as "inner_reason", max("libertybans_applicable_active"."scope_type") as "inner_scope_type",
    max("libertybans_applicable_active"."scope") as "inner_scope", max("libertybans_applicable_active"."start") as "inner_start",
    max("libertybans_applicable_active"."end") as "inner_end", max("libertybans_applicable_active"."track") as "inner_track", "id"
  from "libertybans_applicable_active"
    join "libertybans_strict_links" on "libertybans_applicable_active"."uuid" = "libertybans_strict_links"."uuid1"
  where (
    ("libertybans_applicable_active"."end" = 0 or "libertybans_applicable_active"."end" > 1621440000)
    and "libertybans_strict_links"."uuid2" = X'406a3a9452ed494b8aec8e9690283dfd'
  ) group by "id"
) as "sq_agg"
order by "inner_start" desc, "id" desc;
```

**Applicability query, STRICT, player's history**

```sql
explain select
  "inner_victim_type" as "victim_type", "inner_victim_uuid" as "victim_uuid", "inner_victim_address" as "victim_address",
  "inner_type" as "type", "inner_operator" as "operator", "inner_reason" as "reason", "inner_scope_type" as "scope_type",
  "inner_scope" as "scope", "inner_start" as "start", "inner_end" as "end", "inner_track" as "track", "id"
from (
  select
    max("victim_type") as "inner_victim_type", max("victim_uuid") as "inner_victim_uuid", max("victim_address") as "inner_victim_address",
    max("libertybans_applicable_history"."type") as "inner_type", max("libertybans_applicable_history"."operator") as "inner_operator",
    max("libertybans_applicable_history"."reason") as "inner_reason", max("libertybans_applicable_history"."scope_type") as "inner_scope_type",
    max("libertybans_applicable_history"."scope") as "inner_scope", max("libertybans_applicable_history"."start") as "inner_start",
    max("libertybans_applicable_history"."end") as "inner_end", max("libertybans_applicable_history"."track") as "inner_track", "id"
  from "libertybans_applicable_history"
    join "libertybans_strict_links" on "libertybans_applicable_history"."uuid" = "libertybans_strict_links"."uuid1"
  where (
    ("libertybans_applicable_history"."end" = 0 or "libertybans_applicable_history"."end" > 1621440000)
    and "libertybans_strict_links"."uuid2" = X'406a3a9452ed494b8aec8e9690283dfd'
  ) group by "id"
) as "sq_agg"
order by "inner_start" desc, "id" desc;
```
