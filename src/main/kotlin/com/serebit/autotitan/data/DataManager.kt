package com.serebit.autotitan.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

val serializer: Gson = GsonBuilder().apply {
    serializeNulls()
}.create()

class DataManager(type: Class<*>) {
    private val parentFolder = File(DataManager::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    private val dataFolder = File("$parentFolder/data/${type.simpleName}").apply { mkdirs() }

    inline fun <reified T : Any> read(fileName: String): T? = read(fileName, T::class.java)

    fun <T : Any> read(fileName: String, type: Class<T>): T? {
        val file = File("$dataFolder/$fileName")
        return if (file.exists()) serializer.fromJson(file.readText(), type) else null
    }

    fun write(fileName: String, obj: Any) = File("$dataFolder/$fileName")
            .apply { createNewFile() }
            .writeText(serializer.toJson(obj))
}