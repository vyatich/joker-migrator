version = "1.0.0"

tasks.withType<JavaCompile>().configureEach {
    options.release = 11
}
