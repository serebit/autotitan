import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.impldep.org.apache.maven.model.Build
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.30"
    id("com.github.johnrengelman.shadow") version "2.0.2"
    id("com.github.ben-manes.versions") version "0.17.0"
    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC6-3"
}

group = "com.serebit"
version = "0.5.0"

description = """AutoTitan is a modular, self-hosted Discord bot built in Kotlin using the Java Discord API."""

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "0.22.3")
    compile(group = "com.serebit", name = "loggerkt", version = "0.2.0")
    compile(group = "net.dv8tion", name = "JDA", version = "3.5.1_342")
    compile(group = "commons-validator", name = "commons-validator", version = "1.6")
    compile(group = "com.sedmelluq", name = "lavaplayer", version = "1.2.56")
    compile(group = "fastily", name = "jwiki", version = "1.5.0")
    compile(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
    compile(group = "com.google.guava", name = "guava", version = "24.0-jre")
    compile(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta1")
    compile(group = "com.github.oshi", name = "oshi-core", version = "3.4.4")
    compile(group = "net.jeremybrooks", name = "knicker", version = "2.4.1")
    testCompile(group = "io.kotlintest", name = "kotlintest", version = "2.0.7")
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        manifest {
            attributes["Main-Class"] = "com.serebit.autotitan.MainKt"
        }
    }

    withType<Jar> {
        isEnabled = false
    }

    withType<GradleBuild> {
        dependsOn("shadowJar")
    }

    withType<Test> {
        environment["AUTOTITAN_TEST_MODE_FLAG"] = "true"
    }
}
