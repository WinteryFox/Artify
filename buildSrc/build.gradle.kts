val kotlin_version: String  = "1.8.10"
val ktor_version: String = "2.2.3"

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = kotlin_version))
    implementation(kotlin("serialization", version = kotlin_version))
    implementation("io.ktor.plugin", "plugin", ktor_version)
    implementation(gradleApi())
    implementation(localGroovy())
}
