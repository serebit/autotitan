package com.serebit.autotitan.api

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.serebit.autotitan.internal.classpathResource
import java.lang.reflect.Type

/*
 kotlinx.serialization needs to do a pass over classes marked with @Serializable during compile time, so because scripts
 don't have access to that, DataManager uses Gson instead.
 */
class DataManager internal constructor(moduleName: String) {
    private val folder by lazy { classpathResource("data/$moduleName").also { it.mkdirs() } }

    /*
     This function *needs* to be reified to work with Gson. Gson has issues with Kotlin generics, so if T is of a
     type that uses generics, and T isn't reified when passed to the TypeToken, Gson fails to cast the JSON to the
     input type and throws an exception. Keep it in.
     */
    inline fun <reified T : Any> read(fileName: String): T? = read(fileName, object : TypeToken<T>() {}.type)

    inline fun <reified T : Any> readOrDefault(fileName: String, defaultValue: () -> T) =
        read(fileName) ?: defaultValue()

    fun <T : Any> read(fileName: String, type: Type): T? = folder.resolve(fileName).let { file ->
        if (file.exists()) serializer.fromJson(file.readText(), type) else null
    }

    fun write(fileName: String, obj: Any) = folder.resolve(fileName)
        .also { it.createNewFile() }
        .writeText(serializer.toJson(obj))

    companion object {
        private val serializer = GsonBuilder().apply {
            registerTypeAdapter(jsonDeserializer { (json, _, _) ->
                when {
                    json.asJsonObject.has("id") -> Emote.Jda(json.asJsonObject["id"].asLong)
                    json.asJsonObject.has("unicode") -> Emote.Unicode(json.asJsonObject["unicode"].asString)
                    else -> null
                }
            })
        }.create()
    }
}

class GuildResourceMap<K, V> : MutableMap<Long, MutableMap<K, V>> by mutableMapOf()

class GuildResourceList<E> : MutableMap<Long, MutableList<E>> by mutableMapOf()