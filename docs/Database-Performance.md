
LibertyBans' performance is strongly related to database query performance. Fast database access equates to fast plugin performance.

This page is mostly useful for large servers looking to ensure efficient operation.

## Performance Factors

Performance largely depends on the database queries issued to check incoming players for bans and chatting players for mutes. Every time a player connects to the server, or sends a chat message, a database query is executed. If these queries take longer, connections and chat messages will become delayed.

To reduce database queries issued for chat messages, LibertyBans uses a mute cache. This prevents players chatting frequently from overwhelming the database. The mute cache keeps track of players who recently chatted and whether they are muted.

# Performance areas of interest

## Database queries

As players log in or start chatting, queries are run to check if they are banned or muted. These queries need to finish quickly since they run often.

A query should be scalable. If a query is scalable, its execution time will not depend on the quantity of data. The query will yield similar results on a database with 100 punishments as on a database with 100,000 punishments.

### Log-ins and mute checks

The configured address strictness directly affects query scalability. The highest strictnesses (STERN, STRICT) have the most overhead.

Log-ins and mute checks are the most frequently issued queries, so this is the most important section by far.

For logins and mute-checking:
1. `LENIENT` and `NORMAL` scale well on all databases and configurations.
2. `STERN` is scalable, but weakly scalable for login checks on cracked networks (with non-default alts registry settings).
3. `STRICT` is weakly scalable.

Large servers are discouraged from using a strictness that does not scale with their database (for example, HSQLDB with the `STRICT` setting).

### Computing player history with `show-applicable-for-history`

If you are using `show-applicable-for-history`, then queries for a user's history scale weakly. If you have a large data set, then the command may be unresponsive when staff use /history.

Note that using traditional pagination and large page numbers (e.g. `/history A248 47`) can also take time to respond. In practice this is not a concern, since individual players rarely have dozens of punishments.

### Other plugins using the API

Note that while the queries inside LibertyBans are fine-tuned where possible, other plugins can also cause LibertyBans to execute queries through API calls. The data requested is arbitrary and in some cases, the resulting queries may not scale well.

An example would be requesting all of a player's *active* punishments through the API. The same problem does not exist with history, and this is a quirk of LibertyBans V1's design.

### Results table

Checking whether a SQL query is scalable involves running `EXPLAIN ...` in the SQL database. We did this for several supported databases, and here are the full results.

Notes:
* The box value is the heaviest query algorithm (range lookup, index merge, full index scan, full table scan) and how many times it appeared. full index scans imply weak scalability, and full table scans imply bad or no scalability.
* The query algorithm gives a rough idea of scalability, but it can change depending on the size of the data set. Databases can make different decisions if you have more rows.
* The algorithm also has different meanings depending on the database. A full table scan on HSQLDB may outperform an index scan on MariaDB, even if it scales worse.
* The database contained sample data appropriate for a medium-size server. There were 57 active bans, 1 mute, and 6 warns; 1,975 historical punishments; and 40,000 name and 40,000 IP address records.
* A medal 🏅 is added when a database surpasses its similar competitors.
* Full queries are listed at the bottom of this page (*Appendix*). Note that I am less familiar with PostgreSQL, so I did not include it here.

| Operation                                                         | HSQLDB 2.7.1   | MariaDB 10.3.39 ubu2004      | MariaDB 12.2.2 ubu2404       | MySQL 8.0.45                 | MySQL 9.6.0                  | 
|-------------------------------------------------------------------|----------------|------------------------------|------------------------------|------------------------------|------------------------------|
| **Login or mute check**                                           |                |                              |                              |                              |                              |
| LENIENT                                                           | full table     | index merge                  | index merge                  | full index                   | full index                   |
| NORMAL                                                            | full table     | full index                   | full index                   | full index                   | full index                   | 
| STERN                                                             | full table x5  | full index x2                | full index x2                | full index x2                | full index x2                | 
| STRICT                                                            | full table x3  | full index                   | full index                   | full index                   | full index                   | 
| **Login to cracked network (non-default alts registry settings)** |                |                              |                              |                              |                              | 
| LENIENT                                                           | full table     | index merge                  | index merge                  | full index                   | full index                   | 
| NORMAL                                                            | full table     | index merge                  | index merge                  | full index                   | full index                   | 
| STERN                                                             | full table x7  | full index x2                | full index x2                | full index x2                | full index x2                | 
| STRICT                                                            | full table x7  | full index x2                | full index x2                | full index x2                | full index x2                | 
| **Compute /history with show-applicable-for-history enabled**     |                |                              |                              |                              |                              | 
| LENIENT                                                           | full table     | index merge                  | index merge                  | range                        | range                        | 
| NORMAL                                                            | full table     | range                        | range                        | range                        | range                        | 
| STERN                                                             | full table x5  | range                        | range                        | range x2                     | range x2                     | 
| STRICT                                                            | full table x3  | full index                   | range 🏅                     | full index                   | full index                   | 
| **Plugin API request for all active punishments**                 |                |                              |                              |                              |                              | 
| LENIENT                                                           | full table x6  | full index                   | full index                   | full index x3                | full index x3                | 
| NORMAL                                                            | full table x12 | full index x3                | full index x3                | full index x3                | full index x3                | 
| STERN                                                             | full table x22 | full table x3, full index x6 | full table x3, full index x6 | full table x3, full index x6 | full table x3, full index x6 | 
| STRICT                                                            | full table x12 | full table x3, full index x3 | full table x3, full index x3 | full table x3                | full index x3 🏅             | 

## Performance Configuration

### Connection Pool Size

Following recommended technical practice, LibertyBans uses a fixed connection pool. By default, the connection pool is quite small with only 6 connections.

You will likely be surprised to find that a pool of 6 connections can satisfy a large server with many players connecting and chatting. For a large network with LibertyBans installed on the proxy, you may wish to increase the connection pool size. If installing on the backend servers, but a low or medium player count (<20) on each backend, you may find a smaller connection pool more optimal.

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

If you are familiar with debugging, it is fairly easy to investigate connection pool load by using either live debugging tools or logging configuration to monitor connection pool events.

## Appendix

### Queries gathered

All queries were run against a database containing sample data provided by BlueTree242.

**Applicability queries, LENIENT**

1. Login or mute check (including cracked network)
2. Player's history
3. All of player's active punishments

```sql
-- 1
explain plan for select
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

-- 2
explain plan for select
  "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_simple_history"
where (("libertybans_simple_history"."victim_type" = 0 and "libertybans_simple_history"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a')
  or ("libertybans_simple_history"."victim_type" = 1 and "libertybans_simple_history"."victim_address" = X'6A087183')
  or ("libertybans_simple_history"."victim_type" = 2 and ("libertybans_simple_history"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a'
    or "libertybans_simple_history"."victim_address" = X'6A087183'
  ))
) order by "start" asc, "id" asc;

-- 3
explain plan for select
  "victim_type", "victim_uuid", "victim_address", "type", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_simple_active"
where (("libertybans_simple_active"."victim_type" = 0 and "libertybans_simple_active"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a')
  or ("libertybans_simple_active"."victim_type" = 1 and "libertybans_simple_active"."victim_address" = X'6A087183')
  or ("libertybans_simple_active"."victim_type" = 2 and ("libertybans_simple_active"."victim_uuid" = X'524a5f914d3e49d8a9c7400d4f59569a'
    or "libertybans_simple_active"."victim_address" = X'6A087183'
  ))
) order by "start" asc, "id" asc;
```

**Applicability queries, NORMAL**

1. Login or mute check
2. Login to cracked network
3. Player's history
4. All of player's active punishments

```sql
-- 1
explain plan for select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from "libertybans_applicable_bans"
where (
  ("end" = 0 or "end" > 1621440000)
  and "libertybans_applicable_bans"."uuid" = X'd46b4b373e584adf8510a691bbbc6b10'
) order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;

-- 2
explain plan for select "libertybans_addresses"."address" from "libertybans_addresses"
where "libertybans_addresses"."uuid" = X'03a27a2a7f5c4bcbb369422de19f741f';

explain plan for select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "start", "end", "track", "id"
from "libertybans_simple_bans"
where (("end" = 0 or "end" > 1621440000)
  and (("libertybans_simple_bans"."victim_type" = 0 and "libertybans_simple_bans"."victim_uuid" = X'69e6377889f848128892a3b0e7b3478d')
    or ("libertybans_simple_bans"."victim_type" = 1 and "libertybans_simple_bans"."victim_address" in (
      X'6FE80A154A3824EF91EE67FFE18A67AD',
      X'A32B077B',
      X'1EF97F89',
      X'C07D66F4',
      X'88D97498C221FD69BC893E0E6F37EC1A',
      X'A689353E',
      X'442B5E15AC6206689353E0EC96A8CB67',
      X'6AFFD004'
    ))
    or ("libertybans_simple_bans"."victim_type" = 2 and ("libertybans_simple_bans"."victim_uuid" = X'69e6377889f848128892a3b0e7b3478d'
      or "libertybans_simple_bans"."victim_address" in (
        X'6FE80A154A3824EF91EE67FFE18A67AD',
        X'A32B077B',
        X'1EF97F89',
        X'C07D66F4',
        X'88D97498C221FD69BC893E0E6F37EC1A',
        X'A689353E',
        X'442B5E15AC6206689353E0EC96A8CB67',
        X'6AFFD004'
      )
    ))
  )
) order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;

-- 3
explain plan for select
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

-- 4
explain plan for select
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

**Applicability queries, STERN**

1. Login or mute check
2. Login to cracked network
3. Player's history
4. All of player's active punishments

```sql
-- 1
explain plan for select
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

-- 2
explain plan for select
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

-- 3
explain plan for select
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
) as "sq_agg" order by "inner_start" asc, "id" asc;

-- 4
explain plan for select
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
) as "sq_agg" order by "inner_start" asc, "id" asc;
```

**Applicability queries, STRICT**

1. Login or mute check
2. Login to cracked network
3. Player's history
4. All of player's active punishments

```sql
-- 1
explain plan for select
  "victim_type", "victim_uuid", "victim_address", "operator", "reason", "scope_type", "scope", "start", "end", "track", "id"
from "libertybans_applicable_bans"
  join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1"
where (("end" = 0 or "end" > 1621440000) and "libertybans_strict_links"."uuid2" = X'd0eda4ffc40e43ed8765794640deb8b3')
order by case "end" when 0 then 9223372036854775807 else "end" end desc limit 1;

-- 2
explain plan for select
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

-- 3
explain plan for select
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

-- 4
explain plan for select
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

**Other resources**

Docker database containers, for testing:
- https://hub.docker.com/r/cockroachdb/cockroach
- https://hub.docker.com/_/mariadb
- https://hub.docker.com/_/mysql
- https://hub.docker.com/_/postgres

Regex to cover UUIDs:
```
cast\('([a-f|0-9]{8})-([a-f|0-9]{4})-([a-f|0-9]{4})-([a-f|0-9]{4})-([a-f|0-9]{12})' as uuid\)
```
