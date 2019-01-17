package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parser.TokenType
import com.serebit.autotitan.api.parser.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

data class CommandTemplate(
    val name: String, val description: String,
    val access: Access,
    val parameterTypes: List<KClass<out Any>>,
    val function: suspend (MessageReceivedEvent, List<Any>) -> Unit
) {
    private val tokenTypes = parameterTypes.map { TokenType.from(it) }.requireNoNulls()

    internal fun build(parent: Group?) = Command(name.toLowerCase(), description, access, parent, tokenTypes, function)
}

internal class Command(
    val name: String, description: String,
    val access: Access,
    parent: Group?,
    val tokenTypes: List<TokenType>,
    private val function: suspend (MessageReceivedEvent, List<Any>) -> Unit
) : Invokeable, CoroutineScope {
    override val coroutineContext = Dispatchers.Default
    val isHidden = access.hidden
    val summary = buildString {
        append("`")
        parent?.let { append("${it.name} ")}
        append("$name ${tokenTypes.joinToString(" ") { "<${it.name}>" }}")
        append("`")
    }

    override val helpField = MessageEmbed.Field(summary, "$description\n${access.description}", false)
    override val helpSignature = buildString {
        append("^")
        parent?.let { append("${it.helpSignature} ") }
        append("(\\Q$name\\E)".toRegex())
        append("$")
    }.toRegex()

    val invocationSignature = buildString {
        append("^")
        parent?.let { append("${it.helpSignature} ") }
        append("(\\Q$name\\E)".toRegex())
        tokenTypes.signature().let { if (it.isNotBlank()) append(" $it") }
        append("$")
    }.toRegex(RegexOption.DOT_MATCHES_ALL)

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>) = launch {
        function(evt, parameters)
    }
}
