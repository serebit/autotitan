package com.serebit.autotitan

import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import com.serebit.loggerkt.LogLevel
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.entities.Game

const val NAME = "AutoTitan"
const val VERSION = "0.4.1"
val config: Configuration = Configuration.generate()

fun main(args: Array<String>) {
    args.find { it in setOf("-d", "--debug") }?.let {
        Logger.level = LogLevel.DEBUG
        Logger.debug("Starting in debug mode.")
    }

    jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener)
        setGame(Game.playing(config.prefix))
    }.let {
        println()
        println("$NAME v$VERSION")
        println("Username:    ${it.selfUser.name}")
        println("Ping:        ${it.ping}ms")
        println("Invite link: ${it.asBot().getInviteUrl()}")
    }
}