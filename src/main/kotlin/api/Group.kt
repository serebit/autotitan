package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parser.TokenType
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

internal data class Group(
    val name: String, val description: String,
    override val helpSignature: Regex,
    val commands: List<Command>
) : Invokeable {
    override val helpField = MessageEmbed.Field(
        name,
        "$description\n${commands.joinToString { it.summary }}",
        false
    )
}

data class GroupTemplate(val name: String, val description: String = "", val defaultAccess: Access) {
    private val commands = mutableListOf<Command>()
    private val signature = "(\\Q$name\\E)".toRegex()

    fun addCommand(
        name: String,
        description: String = "",
        access: Access = defaultAccess,
        parameterTypes: List<KClass<out Any>> = emptyList(),
        function: suspend MessageReceivedEvent.(List<Any>) -> Unit
    ) = parameterTypes.map(TokenType.Companion::from).let { tokenTypes ->
        require(null !in tokenTypes)
        require(name.isNotBlank())
        commands += Command(
            name.toLowerCase(), description, access, signature, tokenTypes.requireNoNulls(), function
        )
    }

    internal fun build() = Group(name.toLowerCase(), description, signature, commands.toList())
}

inline fun GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.() -> Unit
) = addCommand(name, description, access, emptyList()) { task() }

inline fun <reified P0> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0) -> Unit
) where P0 : Any = addCommand(name, description, access, listOf(P0::class)) { args ->
    task(args[0] as P0)
}

inline fun <reified P0, reified P1> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1) -> Unit
) where P0 : Any, P1 : Any = addCommand(name, description, access, listOf(P0::class, P1::class)) { args ->
    task(args[0] as P0, args[1] as P1)
}

inline fun <reified P0, reified P1, reified P2> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2) -> Unit
) where P0 : Any, P1 : Any, P2 : Any =
    addCommand(name, description, access, listOf(P0::class, P1::class, P2::class)) { args ->
        task(args as P0, args[1] as P1, args[2] as P2)
    }

inline fun <reified P0, reified P1, reified P2, reified P3> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any =
    addCommand(name, description, access, listOf(P0::class, P1::class, P2::class, P3::class)) { args ->
        task(args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
    }

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any = addCommand(
    name, description, access, listOf(P0::class, P1::class, P2::class, P3::class, P4::class)
) { args ->
    task(args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
}

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> GroupTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4, P5) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any, P5 : Any = addCommand(
    name, description, access,
    listOf(P0::class, P1::class, P2::class, P3::class, P4::class, P5::class)
) { args ->
    task(args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
}
