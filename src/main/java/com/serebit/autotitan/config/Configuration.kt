package com.serebit.autotitan.config

import com.google.gson.Gson
import net.dv8tion.jda.core.entities.User
import java.io.File
import java.util.*

object Configuration {
    private val parentFolder = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile
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
    val oAuthConsumerKey: String
    val oAuthConsumerSecret: String
    val oAuthAccessToken: String
    val oAuthAccessTokenSecret: String

    init {
        if (file.exists()) {
            val configData = Gson().fromJson(file.readText(), ConfigurationData::class.java)
            token = configData.token
            prefix = configData.prefix
            blackList = configData.blackList
            oAuthConsumerKey = configData.oAuthConsumerKey
            oAuthConsumerSecret = configData.oAuthConsumerSecret
            oAuthAccessToken = configData.oAuthAccessToken
            oAuthAccessTokenSecret = configData.oAuthAccessTokenSecret
        } else {
            token = prompt("Enter new token:")
            prefix = prompt("Enter new command prefix:")
            blackList = mutableSetOf()
            oAuthConsumerKey = prompt("Enter Twitter OAuth Consumer Key:")
            oAuthConsumerSecret = prompt("Enter Twitter OAuth Consumer Secret:")
            oAuthAccessToken = prompt("Enter Twitter OAuth Access Token:")
            oAuthAccessTokenSecret = prompt("Enter Twitter OAuth Access Token Secret:")
            serialize()
        }
    }

    private fun serialize() {
        file.parentFile.mkdirs()
        file.writeText(Gson().toJson(ConfigurationData(
                token,
                prefix,
                blackList,
                oAuthConsumerKey,
                oAuthConsumerSecret,
                oAuthAccessToken,
                oAuthAccessTokenSecret
        )))
    }

    private fun prompt(text: String): String {
        print("$text\n> ")
        val input = Scanner(System.`in`).nextLine().trim()
        return if (input.contains("\n") || input.contains(" ")) {
            prompt(text)
        } else input
    }

    private data class ConfigurationData(
            val token: String,
            val prefix: String,
            val blackList: MutableSet<User>,
            val oAuthConsumerKey: String,
            val oAuthConsumerSecret: String,
            val oAuthAccessToken: String,
            val oAuthAccessTokenSecret: String
    )
}
