
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

## The distribution mechanism - dependency downloading

Because of hosting limits, LibertyBans uses some tricks to ensure the distributed jar is less than 5 MB in size. Sha512 hashes are used to ensure the data is exactly the same as expected.

The bootstrap module downloads the implementation jar at runtime. The implementation jar is shaded with all dependencies at compile time and placed in an isolated classloader at runtime.

### Compiling and running the current source

The jar at `bans-distribution/executable/target/LibertyBans-Executable_version.jar` can be run as a plugin on any supported platform.

This jar does NOT relocate shaded dependencies, so it should NOT be used on a production server. It's meant for easy testing and development.

### Compiling and running the latest released version

Use the jar at `bans-distribution/distributable/target/LibertyBans_version.jar`. This is the same jar which is uploaded to release pages like Github Releases.

This jar will downloaded the released version it corresponds to at runtime.

## Testing

You are encouraged to write unit and integration tests!

### Unit tests

Unit tests are run as part of the Maven build. `mvn test` will execute them.

### Integration tests

Using `mvn verify` will build and run all tests, including integration tests. This is the same command used by the Github Actions workflow (CI).

The integration tests rely on [MariaDB4j](https://github.com/vorburger/MariaDB4j). Sometimes MariaDB4j is a little clumsy and requires extra setup on MacOS.

If you would prefer not to run the integration tests yourself, simply let the CI take care of it.

### Manual testing

See the section "Compiling and running the current source"

### Making a release

I make releases with a few steps:

1. `mvn versions:set -DnewVersion={theNextVersion}`
2. `build/check-hashes.sh` to see the new dependency hashes for internal dependencies
3. Update the dependency hashes in the parent pom
4. Perform the deployment with `mvn clean deploy -Pcheck-hash`

The maven-release-plugin does not fit this project unfortunately. I do recommend it for nearly everything else however.
