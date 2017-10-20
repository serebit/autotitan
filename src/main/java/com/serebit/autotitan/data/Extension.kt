package com.serebit.autotitan.data

import com.google.common.reflect.ClassPath

val extensions = ClassPath
        .from(Thread.currentThread().contextClassLoader)
        .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
        .mapNotNull { generateExtension(it.load()) }

val commands = extensions.map { instance ->
    instance::class.java.methods.mapNotNull { Command.generate(instance, it) }
}.flatten().toSet()

val listeners = extensions.map { instance ->
    instance::class.java.methods.mapNotNull { Listener.generate(instance, it) }
}.flatten().toSet()

private fun <T> generateExtension(extension: Class<T>): T? {
    if (extension.constructors.none { it.parameterCount == 0 }) return null
    if (extension.methods.none { Command.isValid(it) || Listener.isValid(it) }) return null
    return extension.getConstructor().newInstance()
}
