
# Contributing

## Pre-requisites

You will need:

1. Git
2. Maven
3. JDK 11 or greater

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

Using `mvn clean verify` will build and run all tests, including integration tests. This is the same command used by the Github Actions workflow (CI).

The integration tests rely on [MariaDB4j](https://github.com/vorburger/MariaDB4j). Sometimes MariaDB4j is a little clumsy; it requires extra setup on MacOS and is not friendly with firewalls.

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
