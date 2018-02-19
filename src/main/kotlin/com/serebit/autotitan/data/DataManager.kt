package com.serebit.autotitan.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import kotlin.reflect.KClass

class DataManager(type: KClass<out Any>) {
    private val serializer: Gson = GsonBuilder().apply {
        serializeNulls()
    }.create()
    private val parentFolder = File(DataManager::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    private val dataFolder = File("$parentFolder/data/${type.simpleName}").also { it.mkdirs() }

    inline fun <reified T : Any> read(fileName: String): T? = read(fileName, T::class)

    fun <T : Any> read(fileName: String, type: KClass<T>): T? {
        val file = File("$dataFolder/$fileName")
        return if (file.exists()) serializer.fromJson(file.readText(), type.java) else null
    }

    fun write(fileName: String, obj: Any) = File("$dataFolder/$fileName")
        .also { it.createNewFile() }
        .writeText(serializer.toJson(obj))
}