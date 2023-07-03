plugins {
    kotlin("jvm")
    sources
}

description = "The core for Artify"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.kotlinx.serialization)
}
