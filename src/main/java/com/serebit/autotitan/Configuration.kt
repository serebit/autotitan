package com.serebit.autotitan

import com.google.gson.Gson
import java.io.File
import java.util.*

val config = Configuration.generate()

class Configuration private constructor(
        val token: String,
        var prefix: String,
        val blackList: MutableSet<Long>
) {
    fun serialize() = file.apply { createNewFile() }.writeText(Gson().toJson(this))

    companion object {
        private val parentFolder = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile
        val file = File("$parentFolder/.config")

        fun generate(): Configuration = if (file.exists()) {
            Gson().fromJson(file.readText(), Configuration::class.java)
        } else {
            Configuration(
                    token = prompt("Enter new token:") { !it.contains("\\s".toRegex()) },
                    prefix = prompt("Enter new command prefix:") { !it.contains("\\s".toRegex()) },
                    blackList = mutableSetOf()
            ).apply { serialize() }
        }

        private fun prompt(text: String, condition: (String) -> Boolean): String {
            print("$text\n> ")
            val input = Scanner(System.`in`).nextLine().trim()
            return if (input.isBlank() || !condition(input)) {
                prompt(text, condition)
            } else input
        }
    }
}
