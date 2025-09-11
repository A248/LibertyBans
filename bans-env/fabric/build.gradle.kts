plugins {
    id("java")
    id("maven-publish")
    id("fabric-loom") version "1.11.8"
}

allprojects {
    version = rootProject.version
    group = rootProject.group
    project.layout.buildDirectory.set(file("target"))

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "fabric-loom")

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    // TODO: Figure out how to put this in subprojects and stop applying fabric-loom to root project
    dependencies {
        minecraft("com.mojang:minecraft:1.21.8")
        mappings("net.fabricmc:yarn:1.21.8+build.1:v2")
        modImplementation("net.fabricmc:fabric-loader:0.16.14")
        modImplementation("net.fabricmc.fabric-api:fabric-api:0.129.0+1.21.8")
    }
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://mvn-repo.arim.space/lesser-gpl3/")
        maven("https://mvn-repo.arim.space/gpl3/")
        maven{
            url = uri("${project.rootDir}/target/local-maven-repo")
            metadataSources {
                mavenPom()
                artifact()
            }
        }
    }

    tasks.processResources {
        filesMatching("fabric.mod.json") {
            expand(rootProject.properties)
        }
    }

    loom {
        runtimeOnlyLog4j = true
    }
}
