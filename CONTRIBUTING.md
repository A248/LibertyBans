
# Building from Source

## Pre-requisites

You will need:

1. Git
2. Maven 3.9.3 or greater
3. JDK 25 or greater

These can be installed through your package manager of choice.

Maven is technically optional and can be substituted with the Maven wrapper - use `./mvnw` instead of `mvn` in commands. This guide will use both commands interchangeably.

## Cloning and Building

Run `git clone https://github.com/A248/LibertyBans.git && cd LibertyBans && mvn package -Pskip-all-tests`

This will clone the source repository and start the Maven build in the cloned directory.

When the build is complete, the jar at `bans-distribution/executable/target/LibertyBans_version.jar` can run as a plugin on any supported platform.

### Build errors (if code modified)

1. A new dependency was declared, and its checksum data needs to be updated. Resolution: Run build/update-checksums.sh and refer to "Adding new dependencies and repositories" below.
```
Missing from sparseDirectory trusted checksum(s) [SHA-512] for artifact
```

2. A new repository was declared, and it needs to be filtered and whitelisted. Resolution: See "Adding new dependencies and repositories" below.
```
Rule 1: org.apache.maven.enforcer.rules.BannedRepositories failed with message:
Current maven session contains banned repository urls, please double check your pom or settings.xml:
```

# Maintenance of the Codebase

## Working on the source code

You can use any IDE you choose. Simply import the project and ensure it is configured to use Maven.

The project is split into several Maven modules. You will want to make sure that your IDE recognizes these modules.

### Code Formatting

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

### Adding new dependencies and repositories

For security reasons, the LibertyBans build requires you to include checksum metadata when declaring new dependencies. The maintainers will review your PR, inspect the dependency, and make sure it matches the expected checksum. Similarly, repositories need to declare which artifact prefixes they will serve.

During the development process, here are ways to make building and testing on your machine easier:
* Run with the unlocked-build profile to allow SNAPSHOT dependency versions and new repository declarations.
  * E.g. `mvn package -Punlocked-build`
  * This will allow you to use SNAPSHOT dependencies and new repositories for now, and you can add metadata later.
* Run the `build/update-checksums.sh` script after adding new stable dependencies.

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
2. `build/prepare-release.sh` to see the new dependency hashes for own-project dependencies
3. Update the dependency hashes in the parent pom
4. Perform the deployment with `./mvnw clean deploy -Pbuild-release,-docker-enabled -DskipTests -Dinvoker.skip=true`
5. Commit and tag the results.

# Build integrity and security

The Minecraft-related ecosystem makes frequent use of private Maven repositories. Many dependencies, including those provided by server platforms, can only be fetched through these repositories. Additionally, many dependencies are `SNAPSHOT` versions, which means that dependency changes can easily slip by unnoticed.

This presents a security vulnerability, especially for big, complicated builds. A compromised repository could easily spread malicious artifacts to builds that depend on it, and even compromise other developers' machines in a chain reaction. We don't want this to happen here, so the LibertyBans build takes steps to protect itself.

## Artifact checksums

SHA-512 checksums are used to guarantee artifact immutability. While this cannot prevent new compromised dependencies from being introduced, it does ensure that the existing build (if unchanged) cannot be compromised.

## Releases

For release dependencies, Maven's [Trusted Checksums](https://maven.apache.org/resolver/expected-checksums.html) prevent dependencies from being used except where their hashes are explicitly written out. See the `.mvn/maven.config` file for these arguments. Developers can use the `build/update-checksums.sh` script to update the checksums when adding new dependencies.

The checksums are stored in the repository under `.mvn/artifact-checksums`. This directory cannot be modified. It can have new files added to it, but if existing files are modified, the Github Action "Artifact checksum preservation" will fail. This Github Action helps prevent maintainers from accidentally merging PRs with checksums of corrupted or malicious artifacts, whether intentional or unintentional.

### Snapshots

For snapshot versions, we enforce checksums manually by placing every snapshot dependency into its own module, under `bans-bootstrap/dependencies`.

This module should be referred to, instead of the original dependency, in code that wants to use the dependency.

### Gradle

The build uses a child process that executes Gradle, to build the Fabric platform. This embedded Gradle build is similarly vulnerable, so Gradle's [dependency checksums feature](https://docs.gradle.org/current/userguide/dependency_verification.html).

Checksums are stored in `gradle/verification-metadata.xml` relative to the Fabric project, and they are updated with `./gradlew --write-verification-metadata sha512`.

Note: Snapshot versions are not checked by Gradle, so they are banned inside the Gradle build.

## Locked snapshots

To enforce snapshot consistency and checksums, snapshots must be listed and locked to a fixed version before their inclusion in the build. A locked snapshot looks like this: `org.spigotmc:spigot-api:1.8.8-R0.1-20160221.082514-43`.

A snapshot dependency with only one used version is referenced in 4 places:
1. In the parent pom's `<dependencyManagement>` section, under the comment "Locked snapshots". The version is defined here as a timestamped snapshot.
2. In the parent pom's `enforce-locked-snapshots` execution of the maven-enforcer-plugin.
3. Declared in the module that verifies its checksum, under `bans-boostrap/dependencies`.
4. Referenced indirectly by the code that uses the dependency.

A snapshot dependency with more than one version in use (for example, spigot-api 1.8.8 and spigot-api 1.16.5) is referenced in 3 logical places:
1. In the parent pom's `enforce-locked-snapshots` execution of the maven-enforcer-plugin, per version.
2. Declared in the module(s) that verifies its checksum, under `bans-boostrap/dependencies`.
3. Referenced indirectly by the code that uses the dependency.

The downstream consumer of a locked snapshot should refer to the module in `bans-boostrap/dependencies`, not the original dependency. The original dependency will be pulled in transitively.

## Repository filtering and prefixes

While not strictly necessary from a security perspective, repository filtering prevents remote repositories from serving unexpected artifacts. For example, the SpigotMC repository shouldn't be responsible for serving an artifact like `slf4j-api`, which is found on Maven Central.

Repositories are managed in two places:
1. In the `.mvn/rrf` directory.
2. In the parent pom's `enforce-restricted-repositories` execution of the maven-enforcer-plugin.

To add a new such repository, follow the pattern in `.mvn/rrf`, then whitelist the repository in the parent pom.
