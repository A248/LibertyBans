
LibertyBans' performance is strongly related to database query performance. Fast database access equates to fast plugin performance.

This page is mostly useful for large servers looking to ensure efficient operation.

### Performance Factors

Performance largely depends on the database queries issued to check incoming players for bans and chatting players for mutes. Every time a player connects to the server, or sends a chat message, a database query is executed. If these queries take longer, connections and chat messages will become delayed.

To reduce database queries issued for chat messages, LibertyBans uses a mute cache. This prevents players chatting frequently from overwhelming the database. The mute cache keeps track of players who recently chatted and whether they are muted.

## Performance Configuration

### Address Strictness

The configured address strictness directly affects query performance. Scalability is the primary concern. If a query is scalable, its execution time will not depend on the quantity of data. For example, a scalable query should yield similar results on a database with 100 punishments as on a database with 100,000 punishments.

Introducing the **Rule of Preservation of Scalability:** In LibertyBans' queries, *scalability* is usually preserved by choosing a stricter level.
  * This point is crucial. It means that query performance is not dependent on the number of stored punishments.
  * There is one exception to the Rule of Preservation of Scalability. Using the default HSQLDB database with `STERN` or `STRICT` eliminates scalability of enforcement queries, as investigated in [#190](https://github.com/A248/LibertyBans/issues/190). Large servers are therefore discouraged from using HSQLDB with either `STERN` or `STRICT` settings.

Generally, progressively stricter address strictness options increase execution time. More computational work is necessary to fulfill a greater level of address strictness. However, thanks to the Rule of Preservation of Scalability, this slightly greater per-query execution cost is relatively non-consequential.

### Connection Pool Size

Following recommended technical practice, LibertyBans uses a fixed connection pool. By default, the connection pool is quite small with only 6 connections.

You will likely be surprised to find that a pool of 6 connections can service a large server with many players connecting and chatting. For a large network with LibertyBans installed on the proxy, you may wish to increase the connection pool size.

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
