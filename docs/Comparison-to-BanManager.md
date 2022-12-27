
This is intended to be an impartial analysis of the API, design, and implementation of BanManager compared to LibertyBans.

BanManager is somewhat unlike the other punishment plugins compared here (AdvancedBan, LibertyBans, LiteBans). It introduces several unique features and creative approaches. This is despite BanManager's much older codebase. As such, BanManager deserves additional credit.

## Design

### Database Schema

The database schemas used by BanManager and LibertyBans are quite similar to each other.
* Both plugins store the raw bytes of UUIDs and IP addresses, rather than using text: BINARY for UUIDs, VARBINARY for IP addresses.
* Both store joining players' UUIDs and IP addresses, so that this information can be retrieved later as needed.
* Both provide seconds-level precision for dates.

Differences: 
* BanManager additionally stores the undoing operator, the staff member who revoked a punishment. This enables staff to see who is responsible for unbans, unmutes, etc. Moreover, BanManager stores *when* the punishment was revoked.
* BanManager stores whether a punishment was made silently.
* Whereas BanManager repeats its schema, LibertyBans' schema is *normalized*, following common practice for relational database schemas.
* LibertyBans uses fewer tables than BanManager to store the same amount of punishments, which may or may not improve code quality.

### API

The BanManager API requires callers to use static access. This decreases the quality of API users' code. Static access creates testing difficulties and makes it harder to reason about initialization order. This in turn will mean developers need to spend more time debugging their use of the BanManager API, which is frustrating and time-consuming for developers.

Moreover, BanManager does not clearly delineate between the API and the implementation. It exposes its API in the same artifact as its common implementation. There is not a clear package separation: Methods in the API package accept classes outside the API package. This can easily lead to plugin developers depending on the internals of BanManager thinking those internals to be part of the API. In turn this would lead to breakage when the internals of BanManager are updated.

Thirdly, BanManager exposes implementation details - such as its use of the ORMLite library - in its API. Coupling the API to a specific implementation library means that if BanManager wants to ever stop using such a library, it will need to break the API, or endure painful work-arounds to preserve compatibility.

In contrast, the LibertyBans API is "object-oriented" following recommended Java practice. The API is clearly defined and completely separate from the implementation.

## Implementation

### Thread-Safety

BanManager has known thread-safety problems.<sup id="note1ret">[1](#note1)</sup> I have not conducted a full analysis of the entire codebase, but it is possible there are other thread-safety bugs than the one cited.

### API Documentation

BanManager exposes API methods which perform database queries without clearly documenting that such methods may block the thread. This may lead to accidental queries on the main thread.

In LibertyBans, all methods which might require an expensive computation return futures, which clearly informs callers of their heavyweight nature.

### Code Quality

Exception handling is an important factor in code quality. Lax exception handling weakens the strength of automated testing, since tests will not fail unless exceptions are correctly thrown. Exceptions help identify bugs quickly and effectively. When exceptions are hidden, developers must spend more time debugging issues, and users must spend more time figuring out why or whether a feature does not work.
* BanManager frequently catches and logs exceptions. A search of the codebase for "printStackTrace()" yielded 291 matches.<sup id="note2ret">[2](#note2)</sup> Also, BanManager sometimes ignores exceptions entirely.
* By contrast, LibertyBans had zero uses of Throwable#printStackTrace - not even in test code. LibertyBans will almost always re-throw exceptions rather than catch them. Valid exceptions are never intentionally ignored.
* Additionally, BanManager sometimes catches NullPointerException, which is considered widely to be a bad practice.

BanManager relies on Lombok for generating methods and constructors. Lombok is a hotly-debated library which uses annotations to automatically add Java code. Although some developers praise lombok because it reduces verbosity, this analysis will consider Lombok a mark of bad code quality. 
* Lombok adds opacity and magic to a Java codebase. Lombok's features require a level of understanding similar to learning new language features.
* Because Lombok relies on JDK internals, it frequently breaks when new Java versions are released. Although this breakage is strictly at compile-time and does not affect the operation of a compiled BanManager jar, it makes contributing to BanManager more difficult because contributors may need to switch JDKs.

### Test Suite

Both plugins run automated tests against their supported databases.

However, BanManager's tendency to catch and log exceptions weakens its tests' effectiveness. Also, BanManager's tests appear to be less extensive than LibertyBans.<sup id="note3ret">[3](#note3)</sup>

## Philosophy

Overall, BanManager and LibertyBans are very similar in terms of development philosophy. Both plugins are distributed at no cost and are open-source.

BanManager and LibertyBans may possibly differ with respect to the process for tackling bugs and implementing new features. Whether to fix bugs before implementing new features, whether to polish existing features before introducing new ones, and how much time should be spent considering the design of new features. However, these are quite subjective metrics.

### Licensing

BanManager's license prohibits commercial use. It is **illegal** to use BanManager for the purpose of monetary profit.<sup id="note4ret">[4](#note4)</sup>

BanManager's license is the Creative Commons Non-Commercial - Share-Alike 2.0 (CC-BY-NC 2.0). In contrast, LibertyBans is licensed under the GNU Affero General Public License, v3 or later (GNU AGPL v3).

Disclaimer: This is not legal advice and the writer is not a lawyer.

## Requirements

### Java Version Support

LibertyBans requires Java 17 whereas BanManager permits Java 8.

### External Databases

Using an external database in either plugin is optional. BanManager uses H2 by default while LibertyBans uses HSQLDB by default.

LibertyBans requires certain minimum versions for database servers. At least MySQL 8.0, MariaDB 10.6, or PostgreSQL 12 is required. Older versions are not supported.

### Platform Support

Both BanManager and LibertyBans support Bukkit and BungeeCord, and different versions of Sponge. Their differences:

* LibertyBans supports Velocity.
* LibertyBans supports Sponge 8, BanManager Sponge 7.
  * The Sponge platforms are *not* compatible with each other. A plugin for Sponge 7 cannot operate on Sponge 8, and vice-versa.

(As of 20 January 2022)

## Features

BanManager has a plethora of features: a wide array of commands and punishment types. The volume of features in BanManager is much more than in LibertyBans.

LibertyBans avoids features it deems unnecessary. However, it has some features which BanManager does not.

### Geyser Support

LibertyBans has full support for Geyser.

BanManager has partial support for Geyser - it assumes the Geyser name prefix is "*" and does not allow configuring this prefix.<sup id="note5ret">[5](#note5)</sup>

### Core punishment types

Both BanManager and LibertyBans include bans, ip-bans, mutes, ip-mutes, and kicks.

LibertyBans provides warnings in a similar manner to other punishments. BanManager's warning feature is a different kind of punishment; warnings cannot be temporary under BanManager, but have "points" which can accumulate.

The following punishment types are exclusive to the plugin listed:
  * BanManager - Notes
  * BanManager - Reports
  * BanManager - Name bans
  * BanManager - Bans of an IP address range
  * LibertyBans - IP-warns and IP-kicks.

LibertyBans supports ip-warns and ip-kicks by nature of its flexible design; it costs nothing to add these extra features.

### Exemption

LibertyBans offers the exemption feature which prevents staff from banning each other. It requires the installation of a backend exemption provider - LuckPerms or Vault - for this feature to operate.

BanManager supports exemption, but players exempted from punishment must be written out in a YML file. The BanManager documentation states "This is required as Bukkit's permission system does not support offline players."<sup id="note6ret">[6](#note6)</sup> This is true, and it is why LibertyBans must depend on third-party permission APIs rather than Bukkit.

### Importing From Other Plugins

BanManager can import from AdvancedBan and vanilla.

LibertyBans is able to import from AdvancedBan, BanManager, LiteBans, and vanilla.

### Converting Storage Backends

BanManager supports converting from H2 to MySQL or MariaDB, but not vice-versa. It is not possible to switch back to H2.<sup id="note7ret">[7](#note7)</sup>

LibertyBans allows the user to swap between any supported storage backend using the self-import feature.

### Additional BanManager features

BanManager has further features which are not described in detail here, such as warning points and various utility commands.

## References

<a id="note1">1</a>: A248. "Race condition in BungeeCord implementation." BanManager Github Issue. https://github.com/BanManagement/BanManager/issues/956 [↩](#note1ret)

<a id="note2">2</a>: A248. Used "rg --stats printStackTrace" on the BanManager source repository @ acb1ef1ebecefb7cfc8b4c255d635ccdb8b57482 and LibertyBans source repository @ f9490d1c8c7c4c59d40df163b8f77faa245efd15. 17 January 2022. [↩](#note2ret)

<a id="note3">3</a>: A248. Analysis of BanManager source repository @ acb1ef1ebecefb7cfc8b4c255d635ccdb8b57482 and LibertyBans source repository @ f9490d1c8c7c4c59d40df163b8f77faa245efd15. 17 January 2022. [↩](#note3ret)

<a id="note4">4</a>: "Creative Commons Legal Code: Attribution-NonCommercial 2.0." *Creative Commons*. Creative Commons. https://creativecommons.org/licenses/by-nc/2.0/legalcode [↩](#note4ret)

<a id="note5">5</a>: BanManager source repository @ acb1ef1ebecefb7cfc8b4c255d635ccdb8b57482. https://github.com/BanManagement/BanManager/blob/acb1ef1ebecefb7cfc8b4c255d635ccdb8b57482/common/src/main/java/me/confuser/banmanager/common/util/StringUtils.java#L130 [↩](#note5ret)

<a id="note6">6</a>: "exemptions.yml." _BanManagement_. https://banmanagement.com/docs/banmanager/configuration/exemptions-yml [↩](#note6ret)

<a id="note7">7</a>: "Migrating from BanManager H2." _BanManagement_. https://banmanagement.com/docs/banmanager/migrations/h2 [↩](#note7ret)

### Disclaimer

Please note that no harm is meant to subjects of criticism. If the writing sounds harsh, we apologize; please let us know and we will make the language less harsh.
