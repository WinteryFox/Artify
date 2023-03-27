val kord_version: String by project
val ktor_version: String by project
val exposed_version: String by project
val logback_version: String by project
val jansi_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
    id("com.github.johnrengelman.shadow")
    sources
}

description = "The API for Artify"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")

    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")

    implementation("com.zaxxer:HikariCP:5.0.1")
    runtimeOnly("org.postgresql:postgresql:42.5.1")
    runtimeOnly("ch.qos.logback:logback-classic:$logback_version")
    runtimeOnly("org.fusesource.jansi:jansi:$jansi_version")
}

project.setProperty("mainClassName", "$group.$name.MainKt")

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.artify.MainKt"
        )
    }
}
