import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.serialization") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.ben-manes.versions") version "0.29.0"
}

group = "com.serebit"
version = "0.7.2"
description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.9")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-core", "1.0.0-RC")
    implementation("com.serebit.logkat", "logkat", "0.6.0")
    implementation("net.dv8tion", "JDA", "4.2.0_187")
    implementation("com.sedmelluq", "lavaplayer", "1.3.50")
    implementation("org.slf4j", "slf4j-simple", "2.0.0-alpha1")
    implementation("com.vdurmont", "emoji-java", "5.1.1")
    implementation("com.github.ajalt", "clikt", "2.8.0")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.11.2")
}

kotlin.sourceSets["main"].languageSettings.apply {
    useExperimentalAnnotation("kotlin.Experimental")
    enableLanguageFeature("InlineClasses")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    shadowJar {
        archiveFileName.set("${archiveBaseName.get()}-${archiveVersion.get()}.${archiveExtension.get()}")
        manifest.attributes["Main-Class"] = "com.serebit.autotitan.MainKt"
    }
}
