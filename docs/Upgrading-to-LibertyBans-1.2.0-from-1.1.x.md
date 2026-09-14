
LibertyBans 1.2.0 is fully compatible with 1.1.x, but please note the following.

## API dependency - Developers Please Read

The API artifact is now deployed to Maven Central as `org.libertybans:bans-api:1.2.0-M1`. No repository declaration is needed on Maven, and `mavenCentral()` is only needed for Gradle.

The repository `mvn-repo.arim.space` should be **REMOVED** from all dependent projects. In 2029, this repository may stop working, and in 2031, it is possible the domain will be retired.

## Multi-instance network compatibility

Before upgrading to 1.2.0, please make sure all instances are running the **latest version** of 1.1.x.

This rule is part of our general policy on version upgrades, so this is just a reminder. Version 1.2.0 accepts longer scope values which could break old versions of 1.1.x.
