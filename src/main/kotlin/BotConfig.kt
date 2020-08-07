package com.serebit.autotitan

import com.serebit.autotitan.internal.classpathResource
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BotConfig(
    val token: String,
    var prefix: String,
    val blackList: MutableSet<Long> = mutableSetOf(),
    val enabledModules: MutableSet<String> = mutableSetOf()
) {
    fun serialize() = file.also { it.createNewFile() }.writeText(Json.encodeToString(this))

    companion object {
        private val file = classpathResource(".config")
        private val dummy by lazy { BotConfig("", "!") }

        fun generate(): BotConfig? = when {
            file.exists() -> Json.decodeFromString(serializer(), file.readText())
            else -> null
        }
    }
}
