plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.ktlint)
}

group = "es.unizar.webeng"
version = "2025-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.logstash.logback.encoder) // JSON logs
    implementation(libs.slf4j.api)
    implementation(libs.jackson.databind)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockito.kotlin)
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Establecemos la propiedad 'spring.profiles.active' para el proceso de testing
    systemProperty("spring.profiles.active", "test")
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
}
