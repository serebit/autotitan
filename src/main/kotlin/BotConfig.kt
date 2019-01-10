package com.serebit.autotitan

import com.serebit.autotitan.data.classpathResource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class BotConfig(
    val token: String,
    var prefix: String,
    val blackList: MutableSet<Long> = mutableSetOf(),
    val enabledModules: MutableSet<String> = mutableSetOf()
) {
    fun serialize() = file.writeText(Json.stringify(serializer(), this))

    companion object {
        private val file = classpathResource(".config").also { it.createNewFile() }
        private val dummy by lazy { BotConfig("", "!") }

        fun generate(): BotConfig = when {
            System.getenv("AUTOTITAN_TEST_MODE_FLAG") == "true" -> dummy
            file.exists() -> Json.parse(serializer(), file.readText())
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
