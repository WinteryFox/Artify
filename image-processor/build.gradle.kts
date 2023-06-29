val logback_version: String by project
val jansi_version: String by project
val amqp_version: String by project
val kotlin_logging_version: String by project
val aws_version: String by project
val coroutines_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    sources
}

description = "The image scaler microservice for Artify"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)

    implementation(libs.bundles.logging)

    implementation(libs.amqp)
    implementation(libs.aws.s3)

    implementation(project(mapOf("path" to ":core")))
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
