package com.serebit.autotitan.api

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.meta.annotations.Module as ModuleAnnotation

fun <T : Any> generateModule(moduleClass: KClass<T>): T? {
    if (moduleClass.findAnnotation<ModuleAnnotation>() == null) return null
    if (moduleClass.constructors.none { it.parameters.isEmpty() }) return null
    @Suppress("UNCHECKED_CAST")
    val hasCommandsOrListeners = moduleClass.declaredFunctions
            .filter { it.returnType.jvmErasure == Unit::class }
            .map { it as KFunction<Unit> }
            .any { Command.isValid(it) || Listener.isValid(it) }
    return if (hasCommandsOrListeners) moduleClass.createInstance() else null
}
