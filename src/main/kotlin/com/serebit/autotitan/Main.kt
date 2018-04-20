package com.serebit.autotitan

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import com.serebit.loggerkt.LogLevel
import com.serebit.loggerkt.Logger
import khttp.get
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.entities.Game
import java.io.FileOutputStream

const val NAME = "AutoTitan"
const val VERSION = "1.0.0"
val config by lazy {
    Configuration.generate()
}

fun main(args: Array<String>) {
    val longFlags = args.filter { it.matches("--\\w+".toRegex()) }
    val subCommands = args.filter { it.matches("\\w+".toRegex()) }

    Logger.level = when {
        "--trace" in longFlags -> LogLevel.TRACE
        "--debug" in longFlags -> LogLevel.DEBUG
        "--info" in longFlags -> LogLevel.INFO
        else -> LogLevel.WARNING
    }

    when {
        "up" in args || "update" in subCommands -> updateAndClose()
    }

    jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener)
        setGame(Game.playing("${config.prefix}help"))
    }.let {
        println(
            """
            $NAME v$VERSION
            Username:    ${it.selfUser.name}
            Ping:        ${it.ping}ms
            Invite link: ${it.asBot().getInviteUrl()}
            """.trimIndent()
        )
    }
}

fun updateAndClose() {
    Gson().fromJson<GithubRelease>(
        get("https://api.github.com/repos/serebit/autotitan/releases/latest").text
    ).assets.firstOrNull { it.content_type == "application/x-java-archive" }?.let {
        get(
            it.url,
            headers = mapOf("Accept" to "application/octet-stream"),
            stream = true
        ).raw.copyTo(FileOutputStream(DataManager.classpath.resolve("autotitan.jar")))

        println("Updated AutoTitan to release ")
    }
    System.exit(0)
}

private data class GithubRelease(val name: String, val tag_name: String, val assets: Set<GithubAsset>)

private data class GithubAsset(val content_type: String, val url: String)
