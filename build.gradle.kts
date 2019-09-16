import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.serebit.strife.buildsrc.ProjectInfo
import com.serebit.strife.buildsrc.Versions
import com.serebit.strife.buildsrc.kotlinx
import com.serebit.strife.buildsrc.ktor
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    id("kotlinx-serialization") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("com.github.ben-manes.versions") version "0.25.0"
}

group = ProjectInfo.group
version = ProjectInfo.version
description = ProjectInfo.description

repositories {
    jcenter()
    kotlinx()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("scripting-jvm-host"))
    implementation(kotlinx("serialization-runtime", version = Versions.SERIALIZATION))
    implementation(kotlinx("coroutines-core", version = Versions.COROUTINES))
    implementation(ktor("client-cio", version = Versions.KTOR))
    implementation(group = "org.slf4j", name = "slf4j-simple", version = Versions.SLF4J)
    implementation(group = "net.dv8tion", name = "JDA", version = Versions.JDA)
    implementation(group = "com.sedmelluq", name = "lavaplayer", version = Versions.LAVAPLAYER)
    implementation(group = "com.serebit.logkat", name = "logkat-jvm", version = Versions.LOGKAT)
    implementation(group = "com.github.oshi", name = "oshi-core", version = Versions.OSHI)
    implementation(group = "com.github.salomonbrys.kotson", name = "kotson", version = Versions.KOTSON)
    implementation(group = "com.github.ajalt", name = "clikt", version = Versions.CLIKT)
}

kotlin.sourceSets["main"].languageSettings.apply {
    useExperimentalAnnotation("kotlin.Experimental")
    enableLanguageFeature("InlineClasses")
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
}
