plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.4.2"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "org.nanawally"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core:3.4.2")
    implementation("io.ktor:ktor-server-netty:3.4.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.2")
    implementation("io.ktor:ktor-server-cors:3.4.2")
    implementation("io.ktor:ktor-server-config-yaml:3.4.2")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.postgresql:postgresql:42.7.11")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.32")

    // Test
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
