package com.serebit.autotitan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.serebit.autotitan.BotConfig
import com.serebit.autotitan.VERSION
import com.serebit.autotitan.listeners.EventDelegate
import com.serebit.logkat.LogLevel
import com.serebit.logkat.Logger
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import java.util.*

class Cli : CliktCommand(name = "autotitan") {
    private val token by option("-t", "--token", help = "Specifies the token to use when logging into Discord.")
    private val prefix by option("-p", "--prefix", help = "Specifies the command prefix to use.")
    private val trace by option("--trace", help = "Enables verbose log output that tracks program execution.")
        .flag()

    init {
        versionOption(VERSION, names = setOf("-v", "--version"))
    }

    override fun run() = runBlocking {
        if (trace) Logger.level = LogLevel.TRACE

        val config = generateConfig(token, prefix)
        val delegate = EventDelegate(config)

        // start loading the modules now, and in the meantime, login to Discord
        val loadModules = delegate.loadModulesAsync()
        JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            addEventListener(delegate)
            setGame(Game.playing("${config.prefix}help"))
        }.build()
        // wait for the module loaders to finish
        loadModules.await()
    }

    private fun generateConfig(token: String?, prefix: String?) = BotConfig.generate() ?: Scanner(System.`in`).use {
        BotConfig(token ?: prompt(it, "Enter token:"), prefix ?: prompt(it, "Enter command prefix:"))
    }

    private tailrec fun prompt(scanner: Scanner, text: String): String {
        print("$text\n> ")
        val input = scanner.nextLine().trim()
        return if (input.isBlank() || input.contains("\\s".toRegex())) {
            prompt(scanner, text)
        } else input
    }
}
