package com.serebit.autotitan.config

import com.google.gson.Gson
import net.dv8tion.jda.core.entities.User
import java.io.File
import java.util.*

private data class ConfigurationData(
        val token: String,
        val prefix: String,
        val blackList: MutableSet<User>
)

object Configuration {
    internal val parentFolder = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    private val file = File("$parentFolder/data/config.json")
    var token: String
        internal set
    var prefix: String = "!"
        set(value) {
            field = when {
                value.isEmpty() -> "!"
                value.length in 1..3 -> value
                else -> value.substring(0..3)
            }
        }
    val blackList: MutableSet<User>

    init {
        if (file.exists()) {
            val configData = Gson().fromJson(file.readText(), ConfigurationData::class.java)
            token = configData.token
            prefix = configData.prefix
            blackList = configData.blackList
        } else {
            token = prompt("Enter new token:")
            prefix = prompt("Enter new command prefix:")
            blackList = mutableSetOf()
            serialize()
        }
    }

    internal fun serialize() {
        file.parentFile.mkdirs()
        file.writeText(Gson().toJson(ConfigurationData(token, prefix, blackList)))
    }

    private fun prompt(text: String): String {
        print("$text\n> ")
        val input = Scanner(System.`in`).nextLine().trim()
        return if (input.contains("\n") || input.contains(" ")) {
            prompt(text)
        } else input
    }
}
