
## Changes

* Require Java 17.
* Using MariaDB with LibertyBans now requires MariaDB 10.6 at minimum.
* Automatic detection of Geyser/Floodgate and determination of the Bedrock name prefix.
  * The `GEYSER` server-type option has been removed. This server type was inflexible; it failed to distinguish between online and offline Geyser-utilizing servers. It is obsolete due to automatic Floodgate detection.
  * The name prefix is queried from the Floodgate API at startup.
* In the rare case that multiple mutes are issued very quickly, the `warn-actions` addon will make sure warns do not "overlap" one another. There were no reports of this happening, but it was a theoretical possibility. The solution works by leveraging the newly-added `seekBefore` API.

### API Changes

* Expand selection capabilities:
  * `SelectionOrder#countNumberOfPunishments` yields the pure number of punishments, which is more efficient than retrieving the punishments themselves from the database.
  * `SelectionOrderBuilder#seekBefore` allows retrieving punishments before a specified time / ID. It is the counterpart of the existing method `SelectionOrderBuilder#seekAfter`.
  * A selection order may filter by victim types, not just victims.