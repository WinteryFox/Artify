plugins {
    `kotlin-dsl`
    id("com.google.cloud.tools.jib") version "3.3.2" apply false
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
    implementation(kotlin("serialization", version = libs.versions.kotlin.get()))
    implementation(libs.ktor.plugin)
    implementation(libs.sonarqube)
    implementation(gradleApi())
    implementation(localGroovy())
}
