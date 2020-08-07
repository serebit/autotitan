package com.serebit.autotitan

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

@Serializable
class Configuration private constructor(
    val token: String,
    var prefix: String,
    val blackList: MutableSet<Long> = mutableSetOf(),
    val enabledModules: MutableSet<String> = mutableSetOf()
) {
    fun serialize() = file.also { it.createNewFile() }.writeText(Json.encodeToString(this))

    companion object {
        private val file = File(this::class.java.protectionDomain.codeSource.location.toURI()).parent.let {
            File("$it/.config")
        }

        fun generate(): Configuration = when {
            file.exists() -> Json.decodeFromString(file.readText())
            else -> Scanner(System.`in`).use { scanner ->
                Configuration(
                    token = prompt(scanner, "Enter new token:") { !it.contains("\\s".toRegex()) },
                    prefix = prompt(scanner, "Enter new command prefix:") { !it.contains("\\s".toRegex()) }
                ).also(Configuration::serialize)
            }
        }

        private tailrec fun prompt(scanner: Scanner, text: String, condition: (String) -> Boolean): String {
            print("$text\n> ")
            val input = scanner.nextLine().trim()
            return if (input.isBlank() || !condition(input)) {
                prompt(scanner, text, condition)
            } else input
        }
    }
}
