val kotlin_version: String by project
val coroutines_version: String by project
val kord_version: String by project
val ktor_version: String by project
val exposed_version: String by project
val logback_version: String by project
val jansi_version: String by project
val amqp_version: String by project
val kotlin_logging_version: String by project
val aws_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
    id("com.github.johnrengelman.shadow")
    id("org.sonarqube") version "4.2.0.3129"
    sources
}

description = "The API for Artify"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlin_logging_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-request-validation:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")

    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")

    implementation(project(mapOf("path" to ":core")))

    implementation("com.amazonaws:aws-java-sdk-s3:$aws_version")
    implementation("com.amazonaws:aws-java-sdk-cognitoidp:$aws_version")

    implementation("com.rabbitmq:amqp-client:$amqp_version")
    implementation("com.zaxxer:HikariCP:5.0.1")

    runtimeOnly("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
    runtimeOnly("org.postgresql:postgresql:42.5.4")
    runtimeOnly("ch.qos.logback:logback-classic:$logback_version")
    runtimeOnly("org.fusesource.jansi:jansi:$jansi_version")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")

    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
}

project.setProperty("mainClassName", "$group.$name.MainKt")

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.artify.MainKt"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

jib {
    to {
        tags = setOf(project.version.toString())
    }
    from.image = "amazoncorretto:19-alpine"
}

sonarqube {
    properties {
        property("sonar.projectKey", "WinteryFox_Artify")
        property("sonar.organization", "winteryfox")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
