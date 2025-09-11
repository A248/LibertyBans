
dependencies {
    implementation("space.arim.libertybans:bans-core:${project.version}")
    implementation(project(path = ":bans-env-fabric-mod", configuration = "namedElements"))
    modImplementation(libs.permissions)
    modImplementation(libs.adventure)
}
