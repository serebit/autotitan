package com.serebit.autotitan

import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import com.serebit.logkat.LogLevel
import com.serebit.logkat.Logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.entities.Game

const val NAME = "AutoTitan"
const val VERSION = "0.5.3"
val config: Configuration = Configuration.generate()

fun main(args: Array<String>) {
    Logger.level = when {
        "--trace" in args -> LogLevel.TRACE
        "--debug" in args -> LogLevel.DEBUG
        "--info" in args -> LogLevel.INFO
        else -> LogLevel.WARNING
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
