import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.impldep.org.apache.maven.model.Build
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.41"
    id("com.github.johnrengelman.shadow") version "2.0.4"
    id("com.github.ben-manes.versions") version "0.17.0"
    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC7"
}

group = "com.serebit"
version = "1.0.0"

description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "0.23.0-dev-1")
    compile(group = "net.dv8tion", name = "JDA", version = "3.6.0_355")
    compile(group = "com.sedmelluq", name = "lavaplayer", version = "1.2.63")
    compile(group = "org.reflections", name = "reflections", version = "0.9.11")
    compile(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
    compile(group = "khttp", name = "khttp", version = "0.1.0")
    compile(group = "com.serebit", name = "loggerkt", version = "0.2.0")
    compile(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta2")
    compile(group = "com.github.oshi", name = "oshi-core", version = "3.5.0")
    testCompile(group = "io.kotlintest", name = "kotlintest", version = "2.0.7")
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

detekt {
    profile("main", Action {
        input = "$projectDir/src/main/kotlin"
        config = "$projectDir/detekt.yml"
        filters = ".*test.*,.*/resources/.*,.*/tmp/.*"
    })
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        archiveName = "$baseName-$version.$extension"
        manifest {
            attributes["Main-Class"] = "com.serebit.autotitan.MainKt"
        }
        exclude {
            it.relativePath.contains("x86") && !it.relativePath.contains("64")
        }
    }

    withType<GradleBuild> {
        dependsOn("shadowJar")
    }

    withType<Test> {
        environment["AUTOTITAN_TEST_MODE_FLAG"] = "true"
    }
}
