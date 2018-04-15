package com.serebit.autotitan.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import kotlin.reflect.KClass

class DataManager(type: KClass<out Any>) {
    private val dataFolder = File("$classpath/data/${type.simpleName}").also { it.mkdirs() }

    /* Uses reified generics to create a TypeToken for the given type */
    inline fun <reified T : Any> read(fileName: String): T? = read(fileName, object : TypeToken<T>() {}.type)

    fun <T : Any> read(fileName: String, type: Type): T? {
        val file = File("$dataFolder/$fileName")
        return if (file.exists()) serializer.fromJson(file.readText(), type) else null
    }

    fun write(fileName: String, obj: Any) = File("$dataFolder/$fileName")
        .also { it.createNewFile() }
        .writeText(serializer.toJson(obj))

    companion object {
        private val classpath = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile
        private val serializer: Gson = GsonBuilder().create()

        fun getResource(path: String) = File("$classpath/resources/$path")
    }
}
