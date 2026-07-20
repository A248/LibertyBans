/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

plugins {
    id("java")
    id("maven-publish")
    id("net.fabricmc.fabric-loom") version "1.17.12"
}

allprojects {
    version = rootProject.version
    group = rootProject.group
    project.layout.buildDirectory.set(file("target"))

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "net.fabricmc.fabric-loom")

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    dependencies {
        minecraft("com.mojang:minecraft:${project.findProperty("minecraft_version")}")
        implementation("net.fabricmc:fabric-loader:${project.findProperty("loader_version")}")
        implementation("net.fabricmc.fabric-api:fabric-api:0.152.1+26.2")
    }
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven {
            url = uri("https://mvn-repo.arim.space/lesser-gpl3/")
            //includeGroupAndSubgroups("space.arim")
        }
        maven {
            url = uri("https://mvn-repo.arim.space/gpl3/")
            //includeGroupAndSubgroups("space.arim")
        }
        maven {
            url = uri("${project.rootDir}/target/maven/local-repo")
            metadataSources {
                mavenPom()
                artifact()
            }
        }
    }

    tasks.processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<Test> {
        systemProperty("log4j2.configurationFile", uri("${project.rootDir}/src/build/log4j2.xml").toString())
    }

    loom {
        runtimeOnlyLog4j = true
    }
}
