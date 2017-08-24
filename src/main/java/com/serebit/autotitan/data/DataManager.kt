package com.serebit.autotitan.data

import com.google.gson.GsonBuilder
import java.io.File

private val parentFolder = File(DataManager::class.java.protectionDomain.codeSource.location.toURI()).parentFile
private val serializer = GsonBuilder().apply {
    serializeNulls()
}.create()

class DataManager(type: Class<*>) {
    private val dataFolder = File("$parentFolder/${type.name.replace(".", "/")}/")

    fun <T> read(fileName: String, objType: Class<T>): T? {
        val file = File("$dataFolder/$fileName")
        return if (file.exists()) serializer.fromJson("$dataFolder/$fileName", objType) else null
    }

    fun write(fileName: String, obj: Any) = File("$dataFolder/$fileName").writeText(serializer.toJson(obj))
}