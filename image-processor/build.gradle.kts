val logback_version: String by project
val jansi_version: String by project
val amqp_version: String by project
val kotlin_logging_version: String by project
val aws_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.cloud.tools.jib")
    id("com.github.johnrengelman.shadow")
    sources
}

description = "The image scaler microservice for Artify"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlin_logging_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    implementation("com.rabbitmq:amqp-client:$amqp_version")
    implementation("com.amazonaws:aws-java-sdk-s3:$aws_version")

    implementation(project(mapOf("path" to ":json")))

    runtimeOnly("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
    runtimeOnly("ch.qos.logback:logback-classic:$logback_version")
    runtimeOnly("org.fusesource.jansi:jansi:$jansi_version")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.artify.MainKt"
        )
    }
}

jib {
    to.image = "winteryfox/artify-image-processor"
    from.image = "amazoncorretto:19-alpine"
}
