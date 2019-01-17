package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parser.Parser
import com.serebit.autotitan.api.parser.TokenType
import com.serebit.autotitan.api.parser.signature
import com.serebit.autotitan.config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

internal data class Command(
    val name: String, val description: String,
    val access: Access,
    private val parentSignature: Regex,
    private val tokenTypes: List<TokenType>,
    private val function: suspend (MessageReceivedEvent, List<Any>) -> Unit
) : Invokeable, CoroutineScope {
    override val coroutineContext = Dispatchers.Default
    val summary = "`$name ${tokenTypes.joinToString(" ") { "<${it.name}>" }}`"
    val isHidden = access.hidden
    override val helpField = MessageEmbed.Field(summary, "$description\n${access.description}", false)
    override val helpSignature = buildString {
        append("^")
        if (parentSignature.pattern.isNotBlank()) append("$parentSignature ")
        append("(\\Q$name\\E)".toRegex())
        append("$")
    }.toRegex()
    private val invocationSignature = buildString {
        append("^")
        if (parentSignature.pattern.isNotBlank()) append("$parentSignature ")
        append("(\\Q$name\\E)".toRegex())
        tokenTypes.signature().let { if (it.isNotBlank()) append(" $it") }
        append("$")
    }.toRegex(RegexOption.DOT_MATCHES_ALL)

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>) = launch {
        function(evt, parameters)
    }

    fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? = if (!access.matches(evt)) {
        null
    } else {
        val tokens = tokenizeMessage(evt.message.contentRaw.removePrefix(config.prefix))
        if (tokens.isNotEmpty()) Parser.castTokens(evt, tokenTypes, tokens.takeLast(tokenTypes.size)) else null
    }

    private fun tokenizeMessage(message: String): List<String> =
        invocationSignature.find(message)?.groups?.mapNotNull { it?.value }?.drop(1) ?: emptyList()
}
