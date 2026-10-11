version = "2.0.0"

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}
