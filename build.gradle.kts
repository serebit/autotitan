import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.ben-manes.versions") version "0.28.0"
}

group = "com.serebit"
version = "0.5.7"

description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.3.5")
    implementation(group = "com.serebit.logkat", name = "logkat-jvm", version = "0.5.2")
    implementation(group = "net.dv8tion", name = "JDA", version = "4.2.0_179")
    implementation(group = "commons-validator", name = "commons-validator", version = "1.6")
    implementation(group = "com.sedmelluq", name = "lavaplayer", version = "1.3.50")
    implementation(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
    implementation(group = "khttp", name = "khttp", version = "1.0.0")
    implementation(group = "com.google.guava", name = "guava", version = "29.0-jre")
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "2.0.0-alpha1")
    implementation(group = "com.github.oshi", name = "oshi-core", version = "5.1.2")
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
