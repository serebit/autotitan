package com.serebit.autotitan

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import java.io.File
import java.util.*

class Configuration private constructor(
    val token: String,
    var prefix: String,
    val blackList: MutableSet<Long> = mutableSetOf(),
    val enabledModules: MutableSet<String> = mutableSetOf()
) {
    fun serialize() = file.also { it.createNewFile() }.writeText(Gson().toJson(this))

    companion object {
        private val file = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile.let {
            File("$it/.config")
        }

        fun generate(): Configuration = when {
            System.getenv("AUTOTITAN_TEST_MODE_FLAG") == "true" -> generateDummy()
            file.exists() -> Gson().fromJson(file.readText())
            else -> Scanner(System.`in`).use { scanner ->
                Configuration(
                    token = prompt(scanner, "Enter new token:") { !it.contains("\\s".toRegex()) },
                    prefix = prompt(scanner, "Enter new command prefix:") { !it.contains("\\s".toRegex()) }
                ).also(Configuration::serialize)
            }
        }

        private fun generateDummy() = Configuration(
            "",
            "!"
        )

        private tailrec fun prompt(scanner: Scanner, text: String, condition: (String) -> Boolean): String {
            print("$text\n> ")
            val input = scanner.nextLine().trim()
            return if (input.isBlank() || !condition(input)) {
                prompt(scanner, text, condition)
            } else input
        }
    }
}
