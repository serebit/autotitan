package com.serebit.autotitan.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import kotlin.reflect.KClass

class DataManager(type: KClass<out Any>) {
    private val dataFolder = classpath.resolve("data/${type.simpleName}").also { it.mkdirs() }

    /* Uses reified generics to create a TypeToken for the given type. Circumvents some issues that Gson has with
    *  generics.
    */
    inline fun <reified T : Any> read(fileName: String): T? = read(fileName, object : TypeToken<T>() {}.type)

    inline fun <reified T : Any> readOrDefault(fileName: String, defaultValue: () -> T) =
        read(fileName) ?: defaultValue()

    fun <T : Any> read(fileName: String, type: Type): T? = dataFolder.resolve(fileName).let { file ->
        if (file.exists()) serializer.fromJson(file.readText(), type) else null
    }

    fun write(fileName: String, obj: Any) = dataFolder.resolve(fileName)
        .also { it.createNewFile() }
        .writeText(serializer.toJson(obj))

    companion object {
        private val codeSource: File = File(this::class.java.protectionDomain.codeSource.location.toURI())
        val classpath: File = codeSource.parentFile
        private val serializer = Gson()

        fun getResource(path: String) = classpath.resolve("resources/$path")
    }
}
