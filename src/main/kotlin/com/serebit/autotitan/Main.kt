package com.serebit.autotitan

import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.listeners.EventListener
import com.serebit.autotitan.network.GithubApi
import com.serebit.autotitan.network.ping
import com.serebit.extensions.contentType
import com.serebit.loggerkt.LogLevel
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import kotlin.system.exitProcess

const val NAME = "AutoTitan"
const val VERSION = "1.0.0"
val config by lazy {
    BotConfig.generate()
}

fun main(args: Array<String>) {
    val longFlags = args.filter { it.matches("--\\w+".toRegex()) }
    val subCommands = args.filter { it.matches("\\w+".toRegex()) }

    Logger.level = when {
        "--trace" in longFlags -> LogLevel.TRACE
        "--debug" in longFlags -> LogLevel.DEBUG
        else -> LogLevel.INFO
    }

    when {
        "up" in args || "update" in subCommands -> {
            update()
            exitProcess(0)
        }
    }

    if (ping("discordapp.com")) {
        JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            addEventListener(EventListener)
            setGame(Game.playing("${config.prefix}help"))
        }.buildBlocking().also {
            println(
                """
                $NAME v$VERSION
                Username:    ${it.selfUser.name}
                Ping:        ${it.ping}ms
                Invite link: ${it.asBot().getInviteUrl()}
                """.trimIndent()
            )
        }
    } else Logger.error("Failed to connect to Discord.")
}

private fun update() = if (DataManager.codeSource.contentType == "application/x-java-archive") {
    GithubApi.getLatestRelease("serebit", "autotitan") { it.tag_name != VERSION }
        ?.assets
        ?.find { it.content_type == "application/x-java-archive" }
        ?.let {
            Logger.info("Updating AutoTitan to latest release...")
            it.streamTo(DataManager.codeSource)
            Logger.info("Finished updating.")
        } ?: Logger.info("No new updates are available.")
} else Logger.error("Can't update unless running from a jarfile.")
