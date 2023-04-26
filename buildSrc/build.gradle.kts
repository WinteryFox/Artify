val kotlin_version: String  = "1.8.21"
val ktor_version: String = "2.3.0"

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
    implementation("com.github.johnrengelman.shadow", "com.github.johnrengelman.shadow.gradle.plugin", "7.1.2")
    implementation("io.ktor.plugin", "plugin", ktor_version)
    implementation("com.google.cloud.tools", "jib-maven-plugin", "3.3.1")
    implementation(gradleApi())
    implementation(localGroovy())
}

allprojects {
    tasks.compileJava {
        sourceCompatibility = "19"
        targetCompatibility = "19"
    }
    tasks.compileKotlin {
        kotlinOptions.jvmTarget = "19"
    }
}
