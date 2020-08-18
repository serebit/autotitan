package com.serebit.autotitan

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.serebit.autotitan.api.logger
import com.serebit.autotitan.internal.EventDelegate
import com.serebit.logkat.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.*

const val NAME = "AutoTitan"
const val VERSION = "0.7.3"

class Cli : CliktCommand(name = "autotitan") {
    private val token by option("-t", "--token", help = "Specifies the token to use when logging into Discord.")
    private val prefix by option("-p", "--prefix", help = "Specifies the command prefix to use.")
    private val trace by option("--trace", help = "Enables verbose log output that tracks program execution.")
        .flag()

    init {
        versionOption(VERSION, names = setOf("-v", "--version"))
    }

    override fun run() = runBlocking {
        if (trace) logger.level = LogLevel.TRACE

        val config = generateConfig(token, prefix)
        val delegate = EventDelegate(config)

        // start loading the modules now, and in the meantime, login to Discord
        val loadModules = delegate.loadModulesAsync()
        CoroutineScope(Dispatchers.IO).launch {
            keepTrying(config, delegate)
        }

        // wait for the module loaders to finish
        loadModules.await()
    }

    private tailrec fun keepTrying(config: BotConfig, delegate: EventDelegate) {
        try {
            JDABuilder.create(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)).apply {
                setToken(config.token)
                addEventListeners(delegate)
                setActivity(Activity.listening("for ${config.prefix}help"))
            }.build()
            return // it worked, get out
        } catch (e: Exception) {
            println("Network failed to initialize, retrying in 10s...")
            Thread.sleep(10000)
        }
        keepTrying(config, delegate) // it didn't work, try again
    }

    private fun generateConfig(token: String?, prefix: String?) = BotConfig.generate()?.let { config ->
        prefix?.let { config.prefix = it }
        token?.let { config.copy(token = it) } ?: config
    } ?: Scanner(System.`in`).use { scanner ->
        BotConfig(
            token ?: prompt(scanner, "Enter token:"),
            prefix ?: prompt(scanner, "Enter command prefix:")
        ).also { it.serialize() }
    }

    private tailrec fun prompt(scanner: Scanner, text: String): String {
        print("$text\n> ")
        val input = scanner.nextLine().trim()
        return if (input.isBlank() || input.contains("\\s".toRegex())) {
            prompt(scanner, text)
        } else input
    }
}

fun main(args: Array<String>) = Cli().main(args)
