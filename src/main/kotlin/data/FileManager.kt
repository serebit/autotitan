package com.serebit.autotitan.data

import java.io.File
import java.nio.file.Files

private object DummyObject

val codeSource = File(DummyObject::class.java.protectionDomain.codeSource.location.toURI())
private val classLoader = DummyObject::class.java.classLoader
private val classpath: File = codeSource.parentFile
private val contentType = Files.probeContentType(codeSource.toPath())

fun classpathResource(path: String) = classpath.resolve(path)

fun internalResource(path: String): File? = if (contentType == "application/x-java-archive") {
    createTempFile().also {
        classLoader.getResourceAsStream(path).copyTo(it.outputStream())
    }
} else File(classLoader.getResource(path).toURI())
