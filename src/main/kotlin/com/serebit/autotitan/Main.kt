package com.serebit.autotitan

import com.serebit.autotitan.apiwrappers.GithubApi
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.listeners.EventListener
import com.serebit.loggerkt.LogLevel
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
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
        "up" in args || "update" in subCommands -> updateAndClose()
    }

    JDABuilder(AccountType.BOT).apply {
        setToken(config.token)
        addEventListener(EventListener)
        setGame(Game.playing("${config.prefix}help"))
    }.buildBlocking().let {
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

private fun updateAndClose(): Nothing {
    when {
        Files.probeContentType(DataManager.codeSource.toPath()) != "application/x-java-archive" ->
            Logger.error("Can't update unless running from a jarfile.")
        !ping("github.com") -> Logger.error("Failed to connect to GitHub.")
        else -> GithubApi.getLatestRelease("serebit", "autotitan")?.let { release ->
            if (release.tag_name != VERSION) {
                release.assets
                    .firstOrNull { it.content_type == "application/x-java-archive" }
                    ?.let {
                        Logger.info("Updating AutoTitan to release ${release.tag_name}...")
                        it.streamTo(DataManager.codeSource)
                        Logger.info("Finished updating.")
                    }
            } else Logger.info("No new updates are available.")
        }
    }
    exitProcess(0)
}

private fun ping(host: String): Boolean = try {
    Socket().use { socket ->
        socket.connect(InetSocketAddress(host, 80), 4000)
        true
    }
} catch (ex: IOException) {
    false
}
