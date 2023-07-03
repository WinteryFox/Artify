plugins {
    kotlin("jvm")
    id(libs.plugins.jib.get().pluginId)
    sources
}

description = "The API for Artify"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)

    implementation(libs.logging.kotlinLogging)
    runtimeOnly(libs.logging.logback)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.serialization)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.autoHeadResponse)
    implementation(libs.ktor.server.auth.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.requestValidation)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    //testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    //testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    implementation(libs.amqp)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikaricp)
    runtimeOnly(libs.postgresql)

    implementation(libs.aws.s3)
    implementation(libs.aws.cognitoidp)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.jupiter)

    implementation(project(mapOf("path" to ":core")))
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "$group.$name.MainKt"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

jib {
    to {
        image = "winteryfox/artify-api"
        tags = setOf(project.version.toString())
    }
    from.image = "amazoncorretto:19-alpine"
}
