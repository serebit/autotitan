import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.20-eap-100"
    id("kotlinx-serialization") version "1.3.20-eap-100"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    id("com.github.ben-manes.versions") version "0.20.0"
}

group = "com.serebit"
version = "1.0.0"

description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    fun kotlinx(name: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$name:$version"

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(kotlin("script-util"))
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlinx("serialization-runtime", version = "0.10.0-eap-1"))
    implementation(kotlinx("coroutines-core", version = "1.1.0"))
    implementation(group = "io.ktor", name = "ktor-client-okhttp", version = "1.1.1")
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta2")
    implementation(group = "net.dv8tion", name = "JDA", version = "3.8.1_439")
    implementation(group = "com.sedmelluq", name = "lavaplayer", version = "1.3.10")
    implementation(group = "com.serebit", name = "logkat-jvm", version = "0.4.2")
    implementation(group = "com.github.oshi", name = "oshi-core", version = "3.12.2")
    implementation(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        archiveFileName.set("$archiveBaseName-$archiveVersion.$archiveExtension")
        manifest.attributes["Main-Class"] = "com.serebit.autotitan.MainKt"
    }

    withType<Test> {
        environment["AUTOTITAN_TEST_MODE_FLAG"] = "true"
    }
}
