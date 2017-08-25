package com.serebit.autotitan.data

class Extension {
    companion object {
        internal fun <T> generate(extension: Class<T>): T? {
            val noParameters = extension.constructors.all { it.parameterCount == 0 }
            val containsCommands by lazy {
                extension.methods.any { Command.isValid(it) }
            }
            val containsListeners by lazy {
                extension.methods.any { Listener.isValid(it) }
            }
            return if (noParameters && (containsCommands || containsListeners)) extension.newInstance() else null
        }
    }
}