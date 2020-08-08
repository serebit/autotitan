package com.serebit.autotitan.internal

import java.io.File

private object DummyObject

private val codeSource = File(DummyObject::class.java.protectionDomain.codeSource.location.toURI())
private val classLoader = DummyObject::class.java.classLoader
private val classpath: File = codeSource.parentFile

internal fun classpathResource(path: String) = classpath.resolve(path)

internal fun internalResource(path: String): File? = createTempFile().also {
    classLoader.getResourceAsStream(path)?.copyTo(it.outputStream())
}
