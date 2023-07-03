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

    implementation(libs.logging.kotlinLogging)
    runtimeOnly(libs.bundles.logback)

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
    to {
        image = "winteryfox/artify-image-processor"
        tags = setOf(project.version.toString())
    }
    from.image = "amazoncorretto:19-alpine"
}
