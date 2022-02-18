
# Building from Source

## Pre-requisites

You will need:

1. Git
2. Maven
3. JDK 17 or greater

These can be installed through your package manager of choice.

## Cloning and Building

Run `git clone https://github.com/A248/LibertyBans.git && cd LibertyBans && mvn package`

This will clone the source repository and start the Maven build in the cloned directory.

When the build is complete, the jar at `bans-distribution/executable/target/LibertyBans_version.jar` can run as a plugin on any supported platform.

# Introduction to the Codebase

## Working on the source code

You can use any IDE you choose. Simply import the project and ensure it is configured to use Maven.

The project is split into several Maven modules. You will want to make sure that your IDE recognizes these modules.

## Architecture

### Project Structure

* The API: `bans-api`
* Startup code which sets up classloader isolation and launches the rest of the plugin: `bans-boostrap`
* Platform-specific plugins:
  * `bans-env-bungeeplugin` (extends Plugin)
  * `bans-env-spigotplugin` (extends JavaPlugin)
  * `bans-env-velocityplugin` (@Plugin)

The following modules comprise the core implementation:

* The core, platform-agnostic implementation: `bans-core`
* Platform-specific implementation code:
  * `bans-env-bungee`
  * `bans-env-spigot`
  * `bans-env-velocity`

### Startup Process

1. LibertyBans starts with the platform-specific plugin, which calls into the boostrap module. 
2. The bootstrap module sets up classloader isolation, downloads or extracts dependencies as necessary, then launches the core.
3. The core creates the configuration and connects to the database.
4. The platform-specific implementation registers commands and listeners.

For example, on Velocity:

```
   bans-env-velocityplugin
          | | | |
          V V V V
       bans-bootstrap
          | | | |               // Plugin class loader
--------- V V V V -------------------------------------
         bans-core              // Isolated class loader
    /       |        \
   /        |         \ 
   V        V          V
Config   Database     bans-env-velocity
```

The implementation modules are placed in an isolated classloader. This classloader separation means that plugin classes are visible to implementation classes, but implementation classes are *not* visible to plugin classes.

## Distribution

LibertyBans is distributed in two ways.

### The release distribution

The release distribution is a lightweight jar which downloads its dependencies at runtime, with SHA-512 hash verification. This jar is published to SpigotMC and Github Releases.

### The development distribution (for compiling and running from source)

The development distribution is intended for compiling and running from source. It uses a nested jar format and extracts these jars at runtime.

### Relocation in Other Plugins

Other plugins must relocate their dependencies for LibertyBans to work properly.

Sometimes, the user's server is bugged -- another plugin did not relocate its dependencies properly. This happens most commonly with HikariCP, a widely-used library.

When this happens, we print a massive warning message and identify the offending plugin.
* For development builds, we fail-fast with an error message.
* For release builds, we attempt to proceed, but we can make no guarantees that LibertyBans will function properly.

## Testing

You are encouraged to write unit and integration tests!

### Unit tests

Unit tests are run as part of the Maven build. `mvn test` will execute them.

### Integration tests

Using `mvn clean verify` will build and run all tests, including integration tests. This is the same command used by the Jenkins CI.

The integration tests optionally rely on docker to start a temporary MariaDB database. Points of note:
 * If you do not have docker installed, and you do not want to install it, that is entirely fine. The build will gracefully skip the tests which require docker. This does not mean no tests will run; some tests contact in-memory databases (like HSQLDB) which do not require docker.
 * The presence of docker is automatically detected on UNIX.
   * If you want to run the tests on Windows, you will need to enable the `docker-enabled` build profile. For example, `mvn clean verify -Pdocker-enabled`.
   * If you want to disable this automatic detection, you can disable the `docker-enabled` build profile. For example, `mvn clean verify -P-docker-enabled`.
 * Random ports are selected for use in the range 40,000-50,000. You may need to tweak your firewall settings accordingly; in some cases you may need to enable outgoing connections on ports `3306` for MySQL and MariaDB, `5432` for PostgreSQL, and `26257` for CockroachDB as well (oddly enough).

If you would prefer not to run the integration tests yourself, that's fine. Simply let the CI take care of it.

### Manual testing

See the section "Compiling and running the current source"

## Making a release

I make releases with a few steps:

1. `mvn versions:set -DnewVersion={theNextVersion}`
2. `build/check-hashes.sh` to see the new dependency hashes for internal dependencies
3. Update the dependency hashes in the parent pom
4. Perform the deployment with `mvn clean deploy -Pcheck-hash`

