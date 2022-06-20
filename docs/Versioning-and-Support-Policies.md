## Versioning

LibertyBans follows semantic versioning, with some additional clarifications as to the separation between the API, server-side usage, and database schema compatibility.

The format of a version is **Major**.**Minor**.**Patch**

The tables indicates possible changes, and the corresponding changes to versioning. For example, `Breaking API change` corresponds to a new major version (ex: 4.0 -> 5.0), so the versioning is 'major version'.

Note that the presence of a version does not necessarily mean that the changes which require that version will be present. For example, there can be a new major version without breaking schema changes.

### API
| Change                          | Versioning Change | Description                                                                                                         |
|---------------------------------|-------------------|---------------------------------------------------------------------------------------------------------------------|
| Breaking API change             | Major             | *Binary*-incompatible or behavior-incompatible changes to the API.                                                  |
| Compatible API feature addition | Minor             | Backwards-binary-compatible and behavior-compatible addition to the API. May be *source*-incompatible in rare cases |
| Implementation bug-fixes        | Patch             | Bugs fixed                                                                                                          |

### Database Schema

Database schema changes only matter if you are doing one of the following:
1. Running on a multi-proxy setup
2. Running a single-proxy setup where LibertyBans is placed on the backend servers
3. Running an external program which connects to the database, for example a web interface

Assuming no external programs are connected to the database, single servers and single-proxy servers where LibertyBans is placed on the proxy do not need to be concerned with database schema changes.

| Change                            | Versioning Change | Description                                                                                                                                                                                                                                                                                                                                                                    |
|-----------------------------------|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Breaking database schema          | Major             | Past versions of LibertyBans will not be able to read or use the new schema                                                                                                                                                                                                                                                                                                    |
| Compatible database schema change | Minor             | **The last minor release** of LibertyBans will be able to read and use the new schema. Previous minor releases of LibertyBans *may NOT* be able to read or use the new schema. For example, this means you can upgrade from 1.2.x to 1.3.x safely, but you *cannot* upgrade from 1.1.x to 1.3.x. You cannot safely have versions 1.1.x and 1.3.x co-existing at the same time. |

### Server-side Usage
| Change                              | Versioning Change | Examples                                                                           |
|-------------------------------------|-------------------|------------------------------------------------------------------------------------|
| Breaking server-side usage          | Major             | Command re-names, permission changes, backwards-incompatible configuration changes |
| Compatible server-side usage change | Minor             | Permission rename, but the old permission is still supported alongside the new one |
| Server-side usage feature addition  | Patch             | New command added, new configuration option                                        |

## Support

| Version                | Support Provided                                           |
|------------------------|------------------------------------------------------------|
| Latest released        | Always                                                     |
| Previous minor version | At least 1 month after the next minor version is released  |
| Previous major version | At least 4 months after the next major version is released |

There is no timeline for responding to support requests, but an effort will be made to respond within 3 business days *at maximum*, ideally within 1 day.

### Support for Previous Versions

* You must be on the most recent version *for that version*. For example, you cannot expect support for 1.4.2 if 1.4.3 has been released.
* Support may be slower than that for the latest version. If you are on the latest version, you are more likely to receive support *faster* and *from more users*.

### A Note on Bugs in Other Software

* If we identify a bug on your server which could affect LibertyBans, we reserve the right to require you to fix this bug before requesting further support.
* If you find a bug in other software which affects LibertyBans; we may ask you to fix this other software, if it is your responsibility to do so.
  * For example, there was a bug in pterodactyl which prevented using pterodactyl, MariaDB, and LibertyBans together. Gepron1x submitted [a fix](https://github.com/pterodactyl/panel/pull/3800) to pterodactyl, but the bug may still have residual impact (old pterodactyl versions or already-created database users).

We are glad to assist you in fixing bugs on your server. The LibertyBans community is reputed for discovering, reporting, and fixing bugs in other plugins. However, the LibertyBans plugin cannot fix these bugs; it can only fix its own bugs.
