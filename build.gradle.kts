
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.20-eap-52"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    id("com.github.ben-manes.versions") version "0.20.0"
}

group = "com.serebit"
version = "1.0.0"

description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
    maven("http://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile(kotlin("script-util"))
    compile(kotlin("compiler-embeddable"))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.1.0")
    compile(group = "net.dv8tion", name = "JDA", version = "3.8.1_439")
    compile(group = "com.sedmelluq", name = "lavaplayer", version = "1.3.10")
    compile(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
    compile(group = "io.ktor", name = "ktor-client-okhttp", version = "1.1.1")
    compile(group = "com.serebit", name = "logkat-jvm", version = "0.4.2")
    compile(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta2")
    compile(group = "com.github.oshi", name = "oshi-core", version = "3.12.1")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        archiveName = "$baseName-$version.$extension"
        manifest.attributes["Main-Class"] = "com.serebit.autotitan.MainKt"
    }

    withType<Test> {
        environment["AUTOTITAN_TEST_MODE_FLAG"] = "true"
    }
}
