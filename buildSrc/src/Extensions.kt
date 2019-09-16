package com.serebit.strife.buildsrc

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun DependencyHandler.kotlinx(module: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$module:$version"
fun DependencyHandler.ktor(module: String, version: String) = "io.ktor:ktor-$module:$version"

fun RepositoryHandler.kotlinx() = maven("https://kotlin.bintray.com/kotlinx")
fun RepositoryHandler.kotlinEap() = maven("https://kotlin.bintray.com/kotlin-eap")
