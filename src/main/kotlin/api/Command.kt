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

internal class Command(
    val name: String, description: String,
    val access: Access,
    private val tokenTypes: List<TokenType>,
    private val function: suspend (MessageReceivedEvent, List<Any>) -> Unit
) : CoroutineScope {
    override val coroutineContext = Dispatchers.Default
    val summary = "`$name ${tokenTypes.joinToString(" ") { "<${it.name}>" }}`"
    val helpField = MessageEmbed.Field(summary, "$description\n${access.description}", false)
    val isHidden = access.hidden
    private val signature = buildString {
        append("^(\\Q$name\\E)".toRegex())
        tokenTypes.signature().let { if (it.isNotBlank()) append(" $it") }
        append("$")
    }.toRegex()

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>) = launch {
        function(evt, parameters)
    }

    fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? = if (!access.matches(evt)) {
        null
    } else {
        val tokens = tokenizeMessage(evt.message.contentRaw.removePrefix(config.prefix))
        if (tokens.isNotEmpty()) Parser.castTokens(evt, tokenTypes, tokens.drop(1)) else null
    }

    private fun tokenizeMessage(message: String): List<String> =
        signature.find(message)?.groups?.mapNotNull { it?.value }?.drop(1) ?: emptyList()
}
