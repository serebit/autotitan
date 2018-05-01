package com.serebit.autotitan

import com.serebit.autotitan.apiwrappers.GithubApi
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import com.serebit.loggerkt.LogLevel
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.entities.Game
import kotlin.system.exitProcess

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
        else -> LogLevel.INFO
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

fun updateAndClose() = GithubApi.getLatestRelease("serebit", "autotitan")?.let { release ->
    if (release.tag_name != VERSION) {
        release.assets
            .firstOrNull { it.content_type == "application/x-java-archive" }
            ?.let {
                it.streamTo(DataManager.codeSource)
                Logger.info("Updated AutoTitan to release ${release.tag_name}")
            }
    } else Logger.info("No new updates are available.")
    exitProcess(0)
}
