package com.serebit.autotitan.api

fun <T : Any> generateModule(extension: Class<T>): T? {
    if (extension.constructors.none { it.parameterCount == 0 }) return null
    if (extension.methods.none { Command.isValid(it) || Listener.isValid(it) }) return null
    return extension.getConstructor().newInstance()
}
