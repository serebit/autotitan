package com.serebit.autotitan.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.serebit.autotitan.api.ModuleTemplate
import java.io.File
import java.lang.reflect.Type

class DataManager(template: ModuleTemplate) {
    private val folder = FileManager.classpathResource("data/${template.name}").also { it.mkdirs() }

    fun <T : Any> read(fileName: String): T? = read(folder.resolve(fileName), object : TypeToken<T>() {}.type)

    inline fun <reified T : Any> readOrDefault(fileName: String, defaultValue: () -> T) =
        read(fileName) ?: defaultValue()

    private fun <T : Any> read(file: File, type: Type): T? =
        if (file.exists()) serializer.fromJson(file.readText(), type) else null

    fun write(fileName: String, obj: Any) = folder.resolve(fileName)
        .also { it.createNewFile() }
        .writeText(serializer.toJson(obj))

    companion object {
        private val serializer = Gson()
    }
}
