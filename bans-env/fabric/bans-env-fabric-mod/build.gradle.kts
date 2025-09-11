
loom {
    mods {
        create(project.properties["plugin_id"] as String) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation("space.arim.libertybans:bans-bootstrap:${project.version}")
    // We bundle the permissions API so that LibertyBans will work out-of-the-box
    include(libs.permissions)
    modImplementation(include(libs.adventure)!!)
}
