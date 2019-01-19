package com.serebit.autotitan.api

import com.serebit.autotitan.BotConfig
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parser.TokenType
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

data class ModuleTemplate(
    val name: String,
    val isOptional: Boolean = false,
    inline val defaultAccess: Access
) {
    lateinit var config: BotConfig
        private set
    val dataManager = DataManager(name)
    private val groups: MutableList<GroupTemplate> = mutableListOf()
    private val commands: MutableList<CommandTemplate> = mutableListOf()
    private val listeners: MutableList<Listener> = mutableListOf()

    fun addGroup(template: GroupTemplate) {
        require(template.name.isNotBlank())
        groups += template
    }

    fun addCommand(template: CommandTemplate) {
        template.parameterTypes.map { TokenType.from(it) }.requireNoNulls()
        require(template.name.isNotBlank())
        commands += template
    }

    fun <T : Event> addListener(eventType: KClass<T>, function: suspend (Event) -> Unit) {
        listeners += Listener(eventType, function)
    }

    internal fun build(config: BotConfig): Module {
        this.config = config
        return Module(name, isOptional, groups, commands, listeners, config)
    }
}

inline fun ModuleTemplate.group(
    name: String,
    description: String = "",
    defaultAccess: Access = this.defaultAccess,
    init: GroupTemplate.() -> Unit
) = addGroup(GroupTemplate(name, description, defaultAccess).apply(init))

inline fun ModuleTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.() -> Unit
) = addCommand(CommandTemplate(name, description, access, emptyList()) { evt, _ -> task(evt) })

inline fun <reified P0> ModuleTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0) -> Unit
) where P0 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class)) { evt, args -> task(evt, args[0] as P0) })

inline fun <reified P0, reified P1> ModuleTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1) -> Unit
) where P0 : Any, P1 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class, P1::class)) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1)
    })

inline fun <reified P0, reified P1, reified P2> ModuleTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2) -> Unit
) where P0 : Any, P1 : Any, P2 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class, P1::class, P2::class)) { evt, args ->
        task(evt, args as P0, args[1] as P1, args[2] as P2)
    })

inline fun <reified P0, reified P1, reified P2, reified P3> ModuleTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any = addCommand(
    CommandTemplate(
        name, description, access,
        listOf(P0::class, P1::class, P2::class, P3::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
    }
)

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> ModuleTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any = addCommand(
    CommandTemplate(
        name, description, access,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
    }
)

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> ModuleTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4, P5) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any, P5 : Any = addCommand(
    CommandTemplate(
        name, description, access,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class, P5::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
    }
)

inline fun <reified T : Event> ModuleTemplate.listener(crossinline task: T.() -> Unit) =
    addListener(T::class) { (it as T).task() }
