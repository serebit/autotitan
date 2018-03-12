package com.serebit.autotitan

import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import com.serebit.loggerkt.LogLevel
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.entities.Game

const val NAME = "AutoTitan"
const val VERSION = "0.5.0"
val config: Configuration = Configuration.generate()

fun main(args: Array<String>) {
    Logger.level = LogLevel.WARNING
    args.find { it == "--trace" }?.let {
        Logger.level = LogLevel.TRACE
    }
    args.find { it == "--debug" }?.let {
        Logger.level = LogLevel.DEBUG
    }
    args.find { it == "--info" }?.let {
        Logger.level = LogLevel.INFO
    }

    jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener)
        setGame(Game.playing("${config.prefix}help"))
    }.let {
        println()
        println("$NAME v$VERSION")
        println("Username:    ${it.selfUser.name}")
        println("Ping:        ${it.ping}ms")
        println("Invite link: ${it.asBot().getInviteUrl()}")
    }
}
