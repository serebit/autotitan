package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.annotations.Module as ModuleAnnotation

fun <T : Any> generateModule(moduleClass: Class<T>): T? {
    if (moduleClass.isAnnotationPresent(ModuleAnnotation::class.java))
    if (moduleClass.constructors.none { it.parameterCount == 0 }) return null
    if (moduleClass.methods.none { Command.isValid(it) || Listener.isValid(it) }) return null
    return moduleClass.getConstructor().newInstance()
}
