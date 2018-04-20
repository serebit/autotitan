package com.serebit.autotitan

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.data.DataManager
import java.util.*

data class Configuration(
    val token: String,
    var prefix: String,
    val blackList: MutableSet<Long> = mutableSetOf(),
    val enabledModules: MutableSet<String> = mutableSetOf()
) {
    fun serialize() = file.also { it.createNewFile() }.writeText(Gson().toJson(this))

    companion object {
        private val file = DataManager.classpath.resolve(".config")

        fun generate(): Configuration = when {
            System.getenv("AUTOTITAN_TEST_MODE_FLAG") == "true" -> dummyConfiguration
            file.exists() -> Gson().fromJson(file.readText())
            else -> Scanner(System.`in`).use { scanner ->
                Configuration(
                    token = prompt(scanner, "Enter new token:") { !it.contains("\\s".toRegex()) },
                    prefix = prompt(scanner, "Enter new command prefix:") { !it.contains("\\s".toRegex()) }
                ).also(Configuration::serialize)
            }
        }

        private val dummyConfiguration by lazy { Configuration("", "!") }

        private tailrec fun prompt(scanner: Scanner, text: String, condition: (String) -> Boolean): String {
            print("$text\n> ")
            val input = scanner.nextLine().trim()
            return if (input.isBlank() || !condition(input)) {
                prompt(scanner, text, condition)
            } else input
        }
    }
}
