import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.40-eap-105"
    id("kotlinx-serialization") version "1.3.40-eap-105"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("com.github.ben-manes.versions") version "0.21.0"
}

group = "com.serebit"
version = "1.0.0-eap"

description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://dl.bintray.com/soywiz/soywiz")
}

dependencies {
    fun kotlinx(name: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$name:$version"

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("scripting-jvm-host"))
    implementation(kotlinx("serialization-runtime", version = "0.11.0"))
    implementation(kotlinx("coroutines-core", version = "1.3.0-M1"))
    implementation(group = "io.ktor", name = "ktor-client-cio", version = "1.2.1")
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta4")
    implementation(group = "net.dv8tion", name = "JDA", version = "3.8.1_439")
    implementation(group = "com.sedmelluq", name = "lavaplayer", version = "1.3.17")
    implementation(group = "com.serebit", name = "logkat-jvm", version = "0.4.5")
    implementation(group = "com.github.oshi", name = "oshi-core", version = "3.13.2")
    implementation(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
    implementation(group = "com.github.ajalt", name = "clikt", version = "2.0.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    withType<ShadowJar> {
        archiveFileName.set("${archiveBaseName.get()}-${archiveVersion.get()}.${archiveExtension.get()}")
        manifest.attributes["Main-Class"] = "com.serebit.autotitan.MainKt"
    }

    withType<Test> {
        environment["AUTOTITAN_TEST_MODE_FLAG"] = "true"
    }
}
