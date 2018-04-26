package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Descriptor
import com.serebit.autotitan.api.parser.Parser
import com.serebit.extensions.jda.canInvokeCommands
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

internal class Command(
    private val descriptor: Descriptor,
    private val access: Access,
    private val delimitLastString: Boolean,
    private val parameterTypes: List<KClass<*>>,
    private val function: (MessageReceivedEvent, List<Any>) -> Unit
) {
    val summary = "`${descriptor.name} ${parameterTypes.joinToString(" ") { "<${it.simpleName}>" }}`"
    val helpField = MessageEmbed.Field(summary, "${descriptor.description}\n${access.description}", false)
    val isHidden get() = access.hidden

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>) = function.invoke(evt, parameters)

    fun matchesName(name: String) = descriptor.name == name

    fun isVisibleFrom(evt: MessageReceivedEvent) = access.matches(evt) && !isHidden

    fun isInvokeableFrom(evt: MessageReceivedEvent) =
        evt.author.canInvokeCommands && access.matches(evt) && descriptor.matches(evt.message.contentRaw)

    fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? {
        val tokens = tokenizeMessage(evt.message.contentRaw)
        return when {
            tokens[0] != descriptor.invocation -> null
            parameterTypes.size != tokens.size - 1 -> null
            else -> parseTokens(evt, tokens).let { parsedTokens ->
                if (parsedTokens.any { it == null }) null else parsedTokens.filterNotNull()
            }
        }
    }

    private fun tokenizeMessage(message: String): List<String> {
        val splitParameters = message.split("\\s+".toRegex()).filter(String::isNotBlank)
        return if (delimitLastString) {
            splitParameters
        } else {
            splitParameters.slice(0 until parameterTypes.size) +
                splitParameters.drop(parameterTypes.size).joinToString(" ")
        }.filter(String::isNotBlank)
    }

    private fun parseTokens(evt: MessageReceivedEvent, tokens: List<String>): List<Any?> =
        parameterTypes.zip(tokens.drop(1)).map { (type, string) ->
            Parser.castToken(evt, type, string)
        }
}
