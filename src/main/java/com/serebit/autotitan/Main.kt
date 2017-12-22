package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.events.Event

const val NAME = "AutoTitan"
const val VERSION = "0.3.3"
lateinit var config: Configuration
    private set
private val modules: List<Module>
    get() = ClassPath
            .from(Thread.currentThread().contextClassLoader)
            .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
            .map { it.load() }
            .filter { it.constructors.any { it.parameterCount == 0 } }
            .map { it.getConstructor().newInstance() as Module }

fun main(args: Array<String>) {
    config = Configuration.generate()
    jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener(modules))
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
        jda.addEventListener(EventListener(modules))
    }
}