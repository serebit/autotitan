package com.serebit.autotitan

import com.serebit.autotitan.internal.classpathResource
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@Serializable
@UseExperimental(UnstableDefault::class)
data class BotConfig(
    val token: String,
    var prefix: String,
    val blackList: MutableSet<Long> = mutableSetOf(),
    val enabledModules: MutableSet<String> = mutableSetOf()
) {
    fun serialize() = file.also { it.createNewFile() }.writeText(Json.stringify(serializer(), this))

    companion object {
        private val file = classpathResource(".config")
        private val dummy by lazy { BotConfig("", "!") }

        fun generate(): BotConfig? = when {
            System.getenv("AUTOTITAN_TEST_MODE_FLAG") == "true" -> dummy
            file.exists() -> Json.parse(serializer(), file.readText())
            else -> null
        }
    }
}
