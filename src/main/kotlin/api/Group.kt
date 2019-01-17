package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parser.TokenType
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

internal class Group(
    val name: String, description: String,
    commandTemplates: List<CommandTemplate>
) : Invokeable {
    val commands = commandTemplates.map { it.build(this) }
    override val helpSignature = "(\\Q$name\\E)".toRegex()
    override val helpField = MessageEmbed.Field(
        "`name`",
        "$description\n${commands.joinToString { it.summary }}",
        false
    )
}

data class GroupTemplate(val name: String, val description: String = "", inline val defaultAccess: Access) {
    private val commands = mutableListOf<CommandTemplate>()

    fun addCommand(template: CommandTemplate) {
        template.parameterTypes.map { TokenType.from(it) }.requireNoNulls()
        require(template.name.isNotBlank())
        commands += template
    }

    internal fun build() = Group(name.toLowerCase(), description, commands)
}

inline fun GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.() -> Unit
) = addCommand(CommandTemplate(name, description, access, emptyList()) { evt, _ -> task(evt) })

inline fun <reified P0> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0) -> Unit
) where P0 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class)) { evt, args -> task(evt, args[0] as P0) })

inline fun <reified P0, reified P1> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1) -> Unit
) where P0 : Any, P1 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class, P1::class)) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1)
    })

inline fun <reified P0, reified P1, reified P2> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2) -> Unit
) where P0 : Any, P1 : Any, P2 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class, P1::class, P2::class)) { evt, args ->
        task(evt, args as P0, args[1] as P1, args[2] as P2)
    })

inline fun <reified P0, reified P1, reified P2, reified P3> GroupTemplate.command(
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

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> GroupTemplate.command(
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

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> GroupTemplate.command(
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
