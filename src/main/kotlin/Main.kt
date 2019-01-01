package com.serebit.autotitan

import com.serebit.autotitan.data.codeSource
import com.serebit.autotitan.listeners.EventDelegate
import com.serebit.autotitan.network.GithubApi
import com.serebit.autotitan.network.ping
import com.serebit.logkat.LogLevel
import com.serebit.logkat.Logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import java.nio.file.Files
import kotlin.system.exitProcess

const val NAME = "AutoTitan"
const val VERSION = "1.0.0"
val config by lazy { BotConfig.generate() }

suspend fun main(args: Array<String>) {
    val longFlags = args.filter { it.matches("--\\w+".toRegex()) }
    val subCommands = args.filter { it.matches("\\w+".toRegex()) }

    Logger.level = when {
        "--trace" in longFlags -> LogLevel.TRACE
        "--debug" in longFlags -> LogLevel.DEBUG
        else -> LogLevel.INFO
    }

    when {
        "up" in subCommands || "update" in subCommands -> {
            update()
            exitProcess(0)
        }
    }

    if (ping("discordapp.com")) {
        // start loading the modules now, and in the meantime, login to Discord
        val loadModules = EventDelegate.loadModulesAsync()
        JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            addEventListener(EventDelegate)
            setGame(Game.playing("${config.prefix}help"))
        }.build()
        // wait for the module loaders to finish
        loadModules.await()
    } else Logger.error("Failed to connect to Discord.")
}

private fun update() = if (Files.probeContentType(codeSource.toPath()) == "application/x-java-archive") {
    GithubApi.getLatestRelease("serebit", "autotitan") { it.tag_name != VERSION }
        ?.assets
        ?.find { it.content_type == "application/x-java-archive" }
        ?.let {
            Logger.info("Updating AutoTitan to latest release...")
            it.streamTo(codeSource)
            Logger.info("Finished updating.")
        } ?: Logger.info("No new updates are available.")
} else Logger.error("Can't update unless running from a jarfile.")
