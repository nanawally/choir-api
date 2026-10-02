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

ktor {
    fatJar {
        archiveFileName.set("choir-api-all.jar")
    }
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
    implementation("io.ktor:ktor-server-auth:3.4.2")
    implementation("io.ktor:ktor-server-auth-jwt:3.4.2")

    // Password hashing
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.ktor:ktor-server-config-yaml:3.4.2")
    implementation("com.charleskorn.kaml:kaml:0.77.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.postgresql:postgresql:42.7.12")

    // Migrations
    implementation("org.flywaydb:flyway-core:11.8.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.2")

    // S3-compatible storage (Neon Object Storage)
    implementation(platform("software.amazon.awssdk:bom:2.31.59"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("ch.qos.logback:logback-core:1.6.3")

    // Test
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
