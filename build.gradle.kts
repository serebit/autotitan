import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("com.github.ben-manes.versions") version "0.25.0"
}

group = "com.serebit"
version = "0.5.5"

description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.3.1")
    implementation(group = "com.serebit.logkat", name = "logkat-jvm", version = "0.4.7")
    implementation(group = "net.dv8tion", name = "JDA", version = "3.8.3_464")
    implementation(group = "commons-validator", name = "commons-validator", version = "1.6")
    implementation(group = "com.sedmelluq", name = "lavaplayer", version = "1.3.22")
    implementation(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
    implementation(group = "khttp", name = "khttp", version = "1.0.0")
    implementation(group = "com.google.guava", name = "guava", version = "28.1-jre")
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "2.0.0-alpha0")
    implementation(group = "com.github.oshi", name = "oshi-core", version = "4.0.0")
    implementation(group = "com.vdurmont", name = "emoji-java", version = "5.1.1")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        archiveFileName.set("${archiveBaseName.get()}-${archiveVersion.get()}.${archiveExtension.get()}")
        manifest.attributes["Main-Class"] = "com.serebit.autotitan.MainKt"
    }

    withType<Test> {
        environment["AUTOTITAN_TEST_MODE_FLAG"] = "true"
    }
}
