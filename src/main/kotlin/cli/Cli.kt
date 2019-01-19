package com.serebit.autotitan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.serebit.autotitan.VERSION
import com.serebit.autotitan.config
import com.serebit.autotitan.listeners.EventDelegate
import com.serebit.logkat.LogLevel
import com.serebit.logkat.Logger
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game

class Cli : CliktCommand() {
    private val trace by option("-t", "--trace", help = "Enables verbose log output that tracks program execution.")
        .flag()

    init {
        versionOption(VERSION, names = setOf("-v", "--version"))
    }

    override fun run() = runBlocking {
        if (trace) Logger.level = LogLevel.TRACE

        // start loading the modules now, and in the meantime, login to Discord
        val loadModules = EventDelegate.loadModulesAsync()
        JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            addEventListener(EventDelegate)
            setGame(Game.playing("${config.prefix}help"))
        }.build()
        // wait for the module loaders to finish
        loadModules.await()
    }
}
