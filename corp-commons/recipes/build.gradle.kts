// Migration recipes for corp-commons, versioned together with the library:
// com.corp:corp-commons-recipes:2.0.0 migrates a service from 1.x to 2.0.
// Declarative only — the jar carries META-INF/rewrite/*.yml and has no code.

version = "2.0.0"

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("recipes") {
            artifactId = "corp-commons-recipes"
            from(components["java"])
        }
    }
}
