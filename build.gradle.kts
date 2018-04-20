import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.impldep.org.apache.maven.model.Build
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.31"
    id("com.github.johnrengelman.shadow") version "2.0.3"
    id("com.github.ben-manes.versions") version "0.17.0"
    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC6-4"
}

group = "com.serebit"
version = "0.5.1"

description = "AutoTitan is a modular, self-hosted Discord bot built in Kotlin/JVM using the Java Discord API."

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "0.22.5")
    compile(group = "com.serebit", name = "loggerkt", version = "0.2.0")
    compile(group = "net.dv8tion", name = "JDA", version = "3.6.0_355")
    compile(group = "commons-validator", name = "commons-validator", version = "1.6")
    compile(group = "com.sedmelluq", name = "lavaplayer", version = "1.2.62")
    compile(group = "com.github.salomonbrys.kotson", name = "kotson", version = "2.5.0")
    compile(group = "khttp", name = "khttp", version = "0.1.0")
    compile(group = "com.google.guava", name = "guava", version = "24.1-jre")
    compile(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta2")
    compile(group = "com.github.oshi", name = "oshi-core", version = "3.4.4")
    compile(group = "net.jeremybrooks", name = "knicker", version = "2.4.1")
    compile(group = "com.vdurmont", name = "emoji-java", version = "4.0.0")
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
    }

    withType<GradleBuild> {
        dependsOn("shadowJar")
    }

    withType<Test> {
        environment["AUTOTITAN_TEST_MODE_FLAG"] = "true"
    }
}
