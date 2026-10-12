// Fictional "internal" library. Both major versions live side by side and are
// published under the same coordinates (com.corp:corp-commons) to ../corp-repo,
// a file-based Maven repository that stands in for the company's artifact server.
// Next to the library, the platform team publishes the migration recipe for each
// major version (com.corp:corp-commons-recipes), so every service team runs the
// same recipe instead of writing its own.

val corpRepo = rootDir.parentFile.resolve("corp-repo")

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "com.corp"

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "corp"
                url = uri(corpRepo)
            }
        }
    }
}

configure(listOf(project(":v1"), project(":v2"))) {
    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("library") {
                artifactId = "corp-commons"
                from(components["java"])
            }
        }
    }
}
