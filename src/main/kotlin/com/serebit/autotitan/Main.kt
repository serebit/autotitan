package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.events.Event
import kotlin.reflect.full.createInstance

const val NAME = "AutoTitan"
const val VERSION = "0.3.3"
val config: Configuration = Configuration.generate()
private val modules: List<Module>
    get() = ClassPath
            .from(Thread.currentThread().contextClassLoader)
            .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
            .mapNotNull { it.load().kotlin.createInstance() as Module }
            .onEach(Module::init)


fun main(args: Array<String>) {
    EventListener.init(modules)
    jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener)
    }.let {
        println()
        println("$NAME v$VERSION")
        println("Username:    ${it.selfUser.name}")
        println("Ping:        ${it.ping}ms")
        println("Invite link: ${it.asBot().getInviteUrl()}")
    }
}

fun resetJda(evt: Event) {
    evt.jda.removeEventListener(EventListener)
    EventListener.init(modules)
    evt.jda.addEventListener(EventListener)
}