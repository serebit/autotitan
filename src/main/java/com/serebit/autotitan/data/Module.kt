package com.serebit.autotitan.data

import com.google.common.reflect.ClassPath

val modules = ClassPath
        .from(Thread.currentThread().contextClassLoader)
        .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
        .mapNotNull { generateModule(it.load()) }

val commands = modules.map { instance ->
    instance::class.java.methods.mapNotNull { Command.generate(instance, it) }
}.flatten().toSet()

val listeners = modules.map { instance ->
    instance::class.java.methods.mapNotNull { Listener.generate(instance, it) }
}.flatten().toSet()

private fun <T> generateModule(extension: Class<T>): T? {
    if (extension.constructors.none { it.parameterCount == 0 }) return null
    if (extension.methods.none { Command.isValid(it) || Listener.isValid(it) }) return null
    return extension.getConstructor().newInstance()
}
