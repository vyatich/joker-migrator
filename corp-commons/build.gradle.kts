// Fictional "internal" library. Both major versions live side by side and are
// published under the same coordinates (com.corp:corp-commons) to ../corp-repo,
// a file-based Maven repository that stands in for the company's artifact server.

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "com.corp"

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
        repositories {
            maven {
                name = "corp"
                url = uri(rootDir.parentFile.resolve("corp-repo"))
            }
        }
    }
}
