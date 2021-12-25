
# Contributing

## Pre-requisites

You will need:

1. Git
2. Maven
3. JDK 11 or greater (JDK 17 is recommended)

These can be installed through your package manager of choice.

## Cloning and Building

Run `git clone https://github.com/A248/LibertyBans.git && cd LibertyBans && mvn package`

This will clone the source repository and start the Maven build in the cloned directory.

## Working on the source code

You can use any IDE you choose. Simply import the project and ensure it is configured to use Maven.

The project is split into several Maven modules:
* The API
* The core, platform-independent implementation
* Implementations for specific platforms
* The bootstrap-related modules, which perform dependency downloading

You will want to make sure that your IDE recognizes these modules.

## The distribution mechanism

### The main distribution which uses dependency downloading

Because of hosting limits, LibertyBans uses some tricks to ensure the main distributed jar, on SpigotMC and Github Releases, is less than 5 MB in size.

Dependencies are downloaded at runtime and verified against SHA-512 hashes. The implementation jar is shaded with all dependencies at compile time and placed in an isolated classloader at runtime.

### The distribution for compiling and running from source

If you want to collaborate on LibertyBans, or build a jar for your own purposes, follow these steps:

1. Use `mvn clean package`.
2. The jar at `bans-distribution/executable/target/LibertyBans-Executable_version.jar` can be run as a plugin on any supported platform.

This jar is fully ready for use. It packages dependencies using a nested jar format, and it will use the sources and libraries you compiled it with.

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
 * Random ports are selected for use in the range 40,000-50,000. You may need to tweak your firewall settings accordingly; in some cases you may need to enable outgoing connections on port(s) `3306` for MySQL and MariaDB, `5432` for PostgreSQL, and `26257` for CockroachDB as well (oddly enough).

If you would prefer not to run the integration tests yourself, that's fine. Simply let the CI take care of it.

### Manual testing

See the section "Compiling and running the current source"

### Making a release

I make releases with a few steps:

1. `mvn versions:set -DnewVersion={theNextVersion}`
2. `build/check-hashes.sh` to see the new dependency hashes for internal dependencies
3. Update the dependency hashes in the parent pom
4. Perform the deployment with `mvn clean deploy -Pcheck-hash`

The maven-release-plugin does not fit this project unfortunately. I do recommend it for nearly everything else however.
