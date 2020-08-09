package com.serebit.autotitan.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.serebit.autotitan.internal.classpathResource

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

data class GuildResourceMap<K, V>(val map: MutableMap<Long, MutableMap<K, V>> = mutableMapOf()) :
    MutableMap<Long, MutableMap<K, V>> by map

data class GuildResourceList<E>(val map: MutableMap<Long, MutableList<E>> = mutableMapOf()) :
    MutableMap<Long, MutableList<E>> by map

val logger = com.serebit.logkat.Logger()
