package com.serebit.autotitan.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.serebit.autotitan.internal.classpathResource
import kotlinx.serialization.Serializable

/*
 kotlinx.serialization needs to do a pass over classes marked with @Serializable during compile time, so because scripts
 don't have access to that, DataManager uses Jackson instead.
 */
class DataManager internal constructor(moduleName: String) {
    val folder by lazy { classpathResource("data/$moduleName").also { it.mkdirs() } }

    inline fun <reified T : Any> read(fileName: String): T? =
        folder.resolve(fileName).let { file ->
            if (file.exists()) mapper.readValue(file.readText()) else null
        }

    inline fun <reified T : Any> readOrDefault(fileName: String, defaultValue: () -> T) =
        read(fileName) ?: defaultValue()

    inline fun <reified T : Any> write(fileName: String, obj: T) = folder.resolve(fileName)
        .also { it.createNewFile() }
        .writeText(mapper.writeValueAsString(obj))

    companion object {
        val mapper = jacksonObjectMapper()
    }
}

@Serializable
class GuildResourceMap<K, V> : MutableMap<Long, MutableMap<K, V>> by mutableMapOf()

@Serializable
class GuildResourceList<E> : MutableMap<Long, MutableList<E>> by mutableMapOf()

val logger = com.serebit.logkat.Logger()
