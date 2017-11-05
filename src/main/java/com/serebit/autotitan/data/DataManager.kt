package com.serebit.autotitan.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

val serializer: Gson = GsonBuilder().apply {
    serializeNulls()
}.create()

class DataManager(type: Class<*>) {
    private val parentFolder = File(DataManager::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    val dataFolder = File("$parentFolder/data/${type.simpleName}").apply { mkdirs() }

    inline fun <reified T> read(fileName: String): T? {
        val file = File("$dataFolder/$fileName")
        return if (file.exists()) serializer.fromJson(file.readText(), T::class.java) else null
    }

    fun write(fileName: String, obj: Any) {
        File("$dataFolder/$fileName").apply { createNewFile() }.writeText(serializer.toJson(obj))
    }
}