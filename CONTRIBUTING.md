
# Building from Source

## Pre-requisites

You will need:

1. Git
2. Maven
3. JDK 21 or greater

These can be installed through your package manager of choice.

## Cloning and Building

Run `git clone https://github.com/A248/LibertyBans.git && cd LibertyBans && mvn package`

This will clone the source repository and start the Maven build in the cloned directory.

When the build is complete, the jar at `bans-distribution/executable/target/LibertyBans_version.jar` can run as a plugin on any supported platform.

# Introduction to the Codebase

## Working on the source code

You can use any IDE you choose. Simply import the project and ensure it is configured to use Maven.

The project is split into several Maven modules. You will want to make sure that your IDE recognizes these modules.

## Code Formatting

Please use tabs. Otherwise, try to follow the surrounding code style.

Try to avoid nesting when working with `CompletableFuture`. If you place callbacks on a new line, the indentation becomes extreme. For example, prefer this:

```java
return selector.getActivePunishmentById(id).thenCompose((optPunishment) -> {
	// Callback
});
```

To this:

```java
return selector.getApplicablePunishment(id)
	.thenCompose((optPunishment) -> {
		// Callback
		// Notice the extra indentation
	});
```

If necessary, break the arguments to the method creating the future onto a new line:

```java
return selector.getApplicablePunishment(
	uuid, address, PunishmentType.BAN
).thenCompose((optPunishment) -> {
	// Callback
});
```

## Architecture

### Project Structure

* The API: `bans-api`
* Startup code which sets up classloader isolation and launches the rest of the plugin: `bans-boostrap`
* Platform-specific plugins:
  * `bans-env-bungeeplugin` (extends Plugin)
  * `bans-env-spigotplugin` (extends JavaPlugin)
  * `bans-env-spongeplugin` (@Plugin)
  * `bans-env-velocityplugin` (@Plugin)

The following modules comprise the core implementation:

* The core, platform-agnostic implementation: `bans-core`
* Platform-specific implementation code:
  * `bans-env-bungee`
  * `bans-env-spigot`
  * `bans-env-sponge`
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

### Addons

Addon modules, under `bans-core-addons`, may be installed at user preference.

Installed addon jars are loaded by the isolated classloader, functioning as if part of `bans-core`.

## Distribution

LibertyBans is distributed in two ways.

### The release distribution

The release distribution is a lightweight jar which downloads its dependencies at runtime, with SHA-512 hash verification. This jar is published to SpigotMC and Github Releases.

### The development distribution

The development distribution is intended for compiling and running from source. It uses a nested jar format and extracts these jars at runtime.

## Testing

You are encouraged to write unit and integration tests!

### Unit tests

Unit tests are run as part of the Maven build. `mvn test` will execute them.

### Integration tests

Using `mvn clean verify` will build and run all tests, including integration tests.

If you prefer not to run the integration tests yourself, that's fine. Simply let the CI take care of it.

**Examples**

Many integration tests rely on docker and can be heavy, so consider disabling the docker detection:
```bash
mvn clean verify -P-docker-enabled
```

Other integration tests use Maven Invoker, which takes a while because it sets up isolated environments. So disable it:
```bash
mvn clean verify -Dinvoker.skip=true -P-docker-enabled
```

To run a specific integration test (e.g. PaginationIT), you can select it:
```bash
mvn clean verify -Dinvoker.skip=true -Dit.test=PaginationIT -P-docker-enabled
```

**Notes on Docker Usage**

If you turn off docker detection, this does not mean no tests will run. Some tests contact in-memory databases (like HSQLDB) which do not require docker.

If you don't turn off docker detection, please keep in mind:
 * Heavy containers will start up. There will be 3 MariaDB containers, 2 MySQL containers, 2 PostgreSQL containers, and 1 CockroachDB container all running on your machine.
 * The presence of docker is automatically detected on UNIX (Mac/Linux). On Windows, you may need to enable the `docker-enabled` profile explicitly. For example, `mvn clean verify -Pdocker-enabled`.
 * Random ports are selected for use in the range 40,000-50,000. Still, you may need to tweak your firewall settings accordingly; in some cases you may need to enable outgoing connections on ports `3306` for MySQL and MariaDB, `5432` for PostgreSQL, and `26257` for CockroachDB as well (oddly enough).

### Logging and debugging

You can configure log levels in `bans-core/src/main/resources/simplelogger.properties` on a per-package basis. Changing the org.jooq logger to 'debug' will enable statement logging.

Using a debugger is possible. You will need to add your debugger's command line arguments to the maven-failsafe plugin configuration. For example:

```xml
<!-- You'll need to merge this section with existing configuration, of course -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <configuration>
    <argLine>-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005</argLine>
  </configuration>
</plugin>
```

### Manual testing

See the section "Cloning and building" for how to obtain a plugin jar.

## Making a release

I make releases with a few steps:

1. `mvn versions:set -DnewVersion={theNextVersion}`
2. `build/check-hashes.sh` to see the new dependency hashes for internal dependencies
3. Update the dependency hashes in the parent pom
4. Perform the deployment with `mvn clean deploy -Pcheck-hash,-docker-enabled -DskipTests -Dinvoker.skip=true`
5. Commit and tag the results.

