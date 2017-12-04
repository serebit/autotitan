package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.api.Command
import com.serebit.autotitan.api.Listener
import com.serebit.autotitan.api.generateModule
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.events.Event

const val NAME = "Spyglass"
const val VERSION = "0.3.2"
lateinit var config: Configuration
private val modules: Pair<List<Command>, List<Listener>>
    get() {
        val instances = ClassPath
                .from(Thread.currentThread().contextClassLoader)
                .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
                .mapNotNull { generateModule(it.load()) }
        val commands = instances.map { instance ->
            instance::class.java.methods.mapNotNull { method -> Command.generate(instance, method) }
        }.flatten()
        val listeners = instances.map { instance ->
            instance::class.java.methods.mapNotNull { method -> Listener.generate(instance, method) }
        }.flatten()
        return commands to listeners
    }

fun main(args: Array<String>) {
    config = Configuration.generate()
    jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener(modules.first, modules.second))
    }.let {
        println()
        println("$NAME v$VERSION")
        println("Username:    ${it.selfUser.name}")
        println("Ping:        ${it.ping}ms")
        println("Invite link: ${it.asBot().getInviteUrl()}")
    }

}

fun resetJda(evt: Event) {
    evt.run {
        jda.registeredListeners.forEach { jda.removeEventListener(it) }
        jda.addEventListener(EventListener(modules.first, modules.second))
    }
}