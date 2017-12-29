package com.serebit.autotitan

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import java.io.File
import java.util.*

class Configuration private constructor(
        val token: String,
        var prefix: String,
        val blackList: MutableSet<Long>,
        var optionalsEnabled: Boolean
) {
    fun serialize() = file.also { it.createNewFile() }.writeText(Gson().toJson(this))

    companion object {
        private val file: File = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile.let {
            File("$it/.config")
        }

        fun generate(): Configuration = when {
            System.getenv("AUTOTITAN_TEST_MODE_FLAG") == "true" -> generateDummy()
            file.exists() -> Gson().fromJson(file.readText())
            else -> Configuration(
                    token = prompt("Enter new token:") { !it.contains("\\s".toRegex()) },
                    prefix = prompt("Enter new command prefix:") { !it.contains("\\s".toRegex()) },
                    blackList = mutableSetOf(),
                    optionalsEnabled = prompt("Enable optional modules? [Y/n]:") {
                        listOf("y", "n").contains(it.toLowerCase())
                    }.let {
                        when (it.toLowerCase()) {
                            "y" -> true
                            "n" -> false
                            else -> throw IllegalStateException("""This should never happen, I take pity on the poor
fool who witnesses this event unfold."""
                            )
                        }
                    }
            ).also(Configuration::serialize)
        }

        private fun generateDummy(): Configuration = Configuration("", "!", mutableSetOf(), true)

        private tailrec fun prompt(text: String, condition: (String) -> Boolean): String {
            print("$text\n> ")
            val input = Scanner(System.`in`).nextLine().trim()
            return if (input.isBlank() || !condition(input)) {
                prompt(text, condition)
            } else input
        }
    }
}
