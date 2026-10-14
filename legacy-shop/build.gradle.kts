plugins {
    java
    id("org.springframework.boot") version "4.0.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "6.6.0"
}

group = "demo"
version = "1.0.0-SNAPSHOT"

tasks.wrapper {
    gradleVersion = "9.1.0"
    distributionType = Wrapper.DistributionType.BIN
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    // the company's artifact server; in the demo, a file repository filled by corp-commons
    maven {
        url = uri("../corp-repo")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.corp:corp-commons:2.0.0")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test:4.0.8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:4.0.8")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
}

// API first: controller interfaces and DTOs are generated from the contract
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/openapi/shop-api.yaml")
    outputDir.set("$buildDir/generated/openapi")
    apiPackage.set("demo.shop.api")
    modelPackage.set("demo.shop.api.model")
    modelNameSuffix.set("Dto")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "useBeanValidation" to "true",
            "openApiNullable" to "false",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "dateLibrary" to "java8",
            "hideGenerationTimestamp" to "true",
            "useSpringBoot3" to "false"
        )
    )
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated/openapi/src/main/java")
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
