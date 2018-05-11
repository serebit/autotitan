package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Descriptor
import com.serebit.autotitan.api.parser.Parser
import com.serebit.autotitan.api.parser.TokenType
import com.serebit.extensions.jda.canInvokeCommands
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

internal class Command(
    private val descriptor: Descriptor,
    private val access: Access,
    private val tokenTypes: List<TokenType>,
    private val function: (MessageReceivedEvent, List<Any>) -> Unit
) {
    val summary = "`${descriptor.name} ${tokenTypes.joinToString(" ") { "<${it.name}>" }}`"
    val helpField = MessageEmbed.Field(summary, "${descriptor.description}\n${access.description}", false)
    val isHidden = access.hidden

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>) = function.invoke(evt, parameters)

    fun matchesName(name: String) = descriptor.name == name

    fun isVisibleFrom(evt: MessageReceivedEvent) = access.matches(evt) && !isHidden

    fun isInvokeableFrom(evt: MessageReceivedEvent) =
        evt.author.canInvokeCommands && access.matches(evt) && descriptor.matches(evt.message.contentRaw)

    fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? {
        val tokens = tokenizeMessage(evt.message.contentRaw)
        return when {
            tokens[0] != descriptor.invocation -> null
            tokenTypes.size != tokens.size - 1 -> null
            else -> parseTokens(evt, tokens).let { parsedTokens ->
                if (parsedTokens.any { it == null }) null else parsedTokens.filterNotNull()
            }
        }
    }

    private fun tokenizeMessage(message: String): List<String> =
        message.split("\\s+".toRegex()).filter(String::isNotBlank).let { splitTokens ->
            (if (tokenTypes.lastOrNull() == TokenType.OtherToken.LongStringToken) {
                splitTokens.slice(0 until tokenTypes.size) + splitTokens.drop(tokenTypes.size).joinToString(" ")
            } else {
                splitTokens
            }).filter(String::isNotBlank)
        }

    private fun parseTokens(evt: MessageReceivedEvent, tokens: List<String>): List<Any?> =
        tokenTypes.zip(tokens.drop(1)).map { (type, string) ->
            Parser.castToken(evt, type, string)
        }
}
