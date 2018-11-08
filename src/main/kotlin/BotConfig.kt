package com.serebit.autotitan

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.data.classpathResource
import java.util.*

data class BotConfig(
    val token: String,
    var prefix: String,
    val blackList: MutableSet<Long> = mutableSetOf(),
    val enabledModules: MutableSet<String> = mutableSetOf()
) {
    fun serialize() = file.also { it.createNewFile() }.writeText(serializer.toJson(this))

    companion object {
        private val serializer = Gson()
        private val file = classpathResource(".config")
        private val dummy by lazy { BotConfig("", "!") }

        fun generate(): BotConfig = when {
            System.getenv("AUTOTITAN_TEST_MODE_FLAG") == "true" -> dummy
            file.exists() -> serializer.fromJson(file.readText())
            else -> Scanner(System.`in`).use { scanner ->
                BotConfig(
                    token = prompt(scanner, "Enter Discord API token:"),
                    prefix = prompt(scanner, "Enter command prefix:")
                ).also(BotConfig::serialize)
            }
        }

        private tailrec fun prompt(scanner: Scanner, text: String): String {
            print("$text\n> ")
            val input = scanner.nextLine().trim()
            return if (input.isBlank() || input.contains("\\s".toRegex())) {
                prompt(scanner, text)
            } else input
        }
    }
}
