
rootProject.name = "bans-env-fabric"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
}

include("bans-env-fabric-mod")
include("bans-env-fabric")
