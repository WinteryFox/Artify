plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
    implementation(kotlin("serialization", version = libs.versions.kotlin.get()))
    implementation(libs.ktor.plugin)
    implementation(libs.jib)
    implementation(libs.sonarqube)
    implementation(gradleApi())
    implementation(localGroovy())
}
