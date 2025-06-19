If you are implementing a web frontend for the LibertyBans database, or otherwise need to connect to the database schema, some things are helpful to know.

Terminology: Throughout this page the adjective terms "standard-compatible", "SQL standard", and "standard" are used to refer to SQL syntax, features, or behavior which is in accordance with the ANSI SQL standard.

Notice: This page documents the schema of LibertyBans 1.0.0. The schema for 0.8.x is similar, but more minimal, and somewhat less normalized: some of the information here does not apply.

## Requirements

LibertyBans needs to run using MariaDB, MySQL, or PostgreSQL. Local storage options (HSQLDB) do not work because they only allow connections from the same process.

## Recommendations

### Write Standard SQL

Since PostgreSQL, MariaDB, and MySQL are quite different in terms of vendor-specific syntax, it is strongly suggested to maintain standard SQL in your code. That way, your program will work on whichever database users happen to choose. Sometimes, however, this may not be possible.

### Use Quoted Identifiers

It is required to use quotes around table and column names. If you do not do this, your SQL queries may fail because some column names are reserved keywords (for example, the `name` column in `libertybans_names`). The rest of this page will show you SQL without quoted identifiers, but it is expected that you quote your own SQL 

For MariaDB and MySQL, you should enable ANSI quotes, and other ANSI SQL behavior, using the sql_mode variable:
* `SET @@SQL_MODE = CONCAT(@@SQL_MODE, ',ANSI');`
* This variable is scoped to the client session. You will need to run the above query every time you open a new connection. Connection pooling libraries should offer this functionality - I know HikariCP (Java) and sqlx (Rust) both do.

### Do Not Blindly Copy and Paste

it is expected that you do not blindly copy and paste from the examples. The examples do not use quoted identifiers. Also, their purpose is tailored to a specific topic, and may have little practical value otherwise.

## Types

### Binary UUIDs

All UUIDs are stored as either BINARY(16) or UUID, depending on the database type. This differs from some other plugins which store UUIDs as text.

* In MariaDB and MySQL, BINARY is used:
  * To convert from CHAR to BINARY, use `HEX`:
    * Note that this requires a UUID without dashes.
    * Example: `SELECT HEX('ed5f12cd600745d9a4b9940524ddaecf') FROM DUAL`
  * To convert from BINARY to CHAR, use `UNHEX`:
    * The returned string will not have dashes.
    * Example: `SELECT UNHEX(uuid) FROM libertybans_names WHERE name = ?`
  * Be careful to avoid full table scans: Inside `WHERE` clauses, you should place the HEX or UNHEX function on the value, not the column:
    * Do this: `SELECT name FROM libertybans_names WHERE uuid = HEX('ed5f12cd600745d9a4b9940524ddaecf')`
    * Do NOT this: `SELECT name FROM libertybans_names WHERE UNHEX(uuid) = 'ed5f12cd600745d9a4b9940524ddaecf'`
* For PostgreSQL, UUIDs are stored as UUID. UUIDs cannot be retrieved as binary; instead, you will need to compare and retrieve them as strings. See also the [PostgreSQL documentation on UUIDs](https://www.postgresql.org/docs/current/datatype-uuid.html).

## Binary IP Addresses`
`
IP addresses are stored as VARBINARY(16) or BYTEA, depending on the database type. For an IPv4 address, 4 bytes are used; for IPv6, 16 bytes.

* VARBINARY is used for MariaDB and MySQL whereas BYTEA is used on PostgreSQL.
* On the JVM, you can use `InetAddress.getByAddress(bytes)` to obtain an `InetAddress` from a binary representation.

## Tables

You may use any database tool of choice to see the tables created.

### Table Information

Details about certain tables and their columns are described here. Some columns in certain tables either do not have an immediately clear meaning, or require additional information to use correctly.

Although MariaDB and PostgreSQL support dialect-specific forms of creating comments on columns, LibertyBans does not use non-standard commenting features. Instead, the meanings of these columns are documented here.

* For the table `libertybans_punishments`, notable columns:
  * `id` - When inserting into this column, values should be retrieved from the sequence `libertybans_punishment_ids`
  * `type` - The punishment type. 0 for bans, 1 for mutes, 2 for warns, 3 for kicks.
  * `operator` - The operator UUID. The zero-valued UUID, a UUID of all zero bytes, represents the console.
  * `start` - a unix timestamp, in seconds, of when the punishment was created
  * `end` - a unix timestamp, in seconds, of when the punishment will end. We guarantee that `end > start`.
* For tables of the form `libertybans_<type>` (the type is 'bans', 'mutes', 'warns', or 'history'):
  * The tables bans, mutes, and warns store active punishments for their respective punishment types.
  * The history table stores all punishments, active and inactive. It is also the only place where kicks are located, since kicks are never active.
  * Columns:
    * `id` - foreign key pointing to the punishment id in `libertybans_punishments`
    * `victim` - foreign key pointing to the victim id in `libertybans_victims`
  * Constraints: For the bans and mutes tables, the victim must be unique. The reason is that there can only be one active ban or active mute for any particular victim.
* For the table `libertybans_victims`:
  * The data in this table is used by the tables storing punishments.
  * Columns:
    * `id` - When inserting into this column, values should be retrieved from the sequence `libertybans_victim_ids`
    * `uuid` and `address` - The meaning and presence of these columns is dependent on the value of the `type` column. Together, they comprise the data representing a victim. They are UUIDs or IP addresses.
    * `type` - the kind of victim which is punished. Possible values and their meanings:
      * `0` - a UUID. `uuid` will be the UUID of the banned player. `address` is unspecified. Corresponds to `VictimType.PLAYER` from the Java API.
      * `1` - an IP address. `address` will be the banned IP address. `uuid` is unspecified. Corresponds to `VictimType.ADDRESS` from the Java API.
      * `2` - a composite UUID and IP address combination. `uuid` will be the banned UUID and `address` will be the banned IP address. Corresponds to `VictimType.COMPOSITE` from the Java API.
* For the tables `libertybans_names` and `libertybans_addresses`:
  * `updated` - a unix timestamps, in seconds, of when the record was created or last updated. In other words, this tells you how up-to-date the entry is.
  * `libertybans_addresses`.`address` - a IPv4 or IPv6 address, guaranteed to be either 4 or 16 bytes long.

### Views

The "simple_" views will help you avoid writing repetitive JOINs. The views are perfect for when you need easily displayable information.

* The `libertybans_simple_<type>` views are the result of joining the `libertybans_punishments`, `libertybans_<type>`, and `libertybans_victims` tables, by punishment ID and victim ID:
  * For example, `liberybans_simple_bans` is the result of joining libertybans_punishments, libertybans_bans, and libertybans_victims.
  * These views will render as an easily accessible table with id, type, victim_type, victim_uuid, victim_address, operator, reason, scope, start, and end columns.
  * The `victim_type`, `victim_uuid`, and `victim_address` columns have the same meaning as the `type`, `uuid`, and `address` columns from the `libertybans_victims` table.
  * The other columns have the same meaning as the columns in the `libertybans_punishments` table.
  * The `libertybans_simple_active` view is a `UNION` of libertybans_simple_bans, libertybans_simple_mutes, libertybans_simple_end
  * Remember to check time-based expiration when you select from these views.
* The `libertybans_simple_history` view is very similar. However, it contains all punishments, not just active punishments.

### Checking Expiration

When selecting active punishments, it is necessary to exclude those punishments which are expired.

To do this, first obtain the current timestamp - a unix timestamp, in seconds. This timestamp is the current time.

Then, add a WHERE predicate to exclude expired punishments:
* `WHERE end = 0 OR end > currentTime` where currentTime is your timestamp (in practice, it is suggested to use bind variables).
  * Explanation: If `end` is `0`, the punishment is permanent. If `end` is greater than the current time, the punishment is temporary but has not yet expired.

### Sequences

You may be familiar with AUTO_INCREMENT in MariaDB/MySQL, SERIAL in PostgreSQL, or GENERATED BY DEFAULT AS IDENTITY in other RDMSes.

The standard-compatible<sup>1</sup> alternative to these dialect-specific features is *sequences*.

LibertyBans has the following sequences:
* `libertybans_punishment_ids` - used to generate punishment IDs. Returns BIGINT
* `libertybans_victim_ids` - used to generate victim IDs. Returns INT

* For MariaDB, use the standard `NEXT VALUE FOR` to obtain the next value for a sequence. To retrieve the last sequene value generated in the client session, use `PREVIOUS VALUE FOR`. See also the [MariaDB docs on sequences](https://mariadb.com/kb/en/create-sequence/)
* For PostgreSQL, `NEXTVAL` has to be used, because PostgreSQL does not support the standard syntax. See the [PostgreSQL docs on sequences](https://www.postgresql.org/docs/current/functions-sequence.html)
* MySQL does not support sequences at all. For MySQL, LibertyBans emulates sequenes using the following strategy:
  * Instead of using a sequence, a table is created with the same name. This will be called the emulation table.
  * The emulation table has a single row, and a single column of importance - the `value` column. The `value` column stores the value of the sequence.
  * To increment the sequence value (emulating `NEXT VALUE FOR` / `NEXTVAL`):
    1. Select the current value into a variable.
    2. Increment the sequence value.
    3. Use the value of the variable from the first step.
  * To obtain the current sequence value (emulating `PREVIOUS VALUE FOR` or `CURRVAL`) you will need to store, in your program, the last value you received from your sequence.
  * To maintain integrity, all interactions with sequences should be done inside transactions, with a transaction isolation level of at least READ_COMMITTED.

Here is an example of incrementing the sequence value.

```sql
SET autocommit = 0;

-- Emulate NEXT VALUE FOR("libertybans_punishment_ids")
START TRANSACTION;
SELECT "value" INTO @@id FROM "libertybans_punishment_ids";
UPDATE "libertybans_punishment_ids" SET "value" = "value" + 1;
COMMIT;

-- Now we may use @@id
```

<sub>1 - While GENERATED BY DEFAULT AS IDENTITY is also standard SQL, it is sadly not supported by MariaDB. Therefore, sequences are used.

### Finding player names for UUIDs

Use the `latest_names` view to look up the most recent known name for a UUID.

## Other Information

### Notes on implementation details

* TINYINT data types cannot be used because they are non-standard.
* Index names need to be unique at the database level, due to [PostgreSQL](https://stackoverflow.com/questions/27306539/at-what-level-do-postgres-index-names-need-to-be-unique)
