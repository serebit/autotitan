package com.serebit.autotitan.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.reflect.KClass

internal class DataManager(type: KClass<out Any>) {
    private val parentFolder = File(DataManager::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    private val dataFolder = File("$parentFolder/data/${type.simpleName}").also { it.mkdirs() }

    /* Uses reified generics to create a TypeToken for the given type */
    inline fun <reified T : Any> read(fileName: String): T? {
        val file = File("$dataFolder/$fileName")
        return if (file.exists()) Json.decodeFromString(file.readText()) else null
    }

    fun write(fileName: String, obj: Any) = File("$dataFolder/$fileName")
        .also { it.createNewFile() }
        .writeText(Json.encodeToString(obj))
}
