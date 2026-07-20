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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat

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

dependencies {
    implementation("space.arim.libertybans:bans-core:${project.version}")
    implementation("space.arim.libertybans:bans-env-adventure5-common:${project.version}")
    implementation(project(path = ":bans-env-fabric-mod"))
    implementation("net.kyori:adventure-platform-fabric:${project.findProperty("adventure_platform_version")}")
    testImplementation("space.arim.api:arimapi-util-testing:${project.findProperty("arimapi_version")}")
    testImplementation("org.junit.jupiter:junit-jupiter:${project.findProperty("junit_version")}")
    testImplementation("org.mockito:mockito-core:${project.findProperty("mockito_version")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Prevent the server's logs/latest.log from being created in our repository
    // Not currently needed because we install our own log4j2.xml file
    //workingDir = file("target")
    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}
