package com.serebit.autotitan.data

import java.io.File

object FileManager {
    val codeSource = File(this::class.java.protectionDomain.codeSource.location.toURI())
    private val classpath: File = codeSource.parentFile

    fun classpathResource(path: String) = classpath.resolve(path)

    fun internalResource(path: String) = codeSource.resolve(path)
}
