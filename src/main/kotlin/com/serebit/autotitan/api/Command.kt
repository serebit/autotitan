package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.config
import com.serebit.autotitan.data.Emote
import com.serebit.extensions.jda.asEmoji
import com.serebit.extensions.jda.getEmoteByMention
import com.serebit.extensions.jda.getMemberByMention
import com.serebit.extensions.jda.getTextChannelByMention
import com.serebit.extensions.jda.getUserByMention
import com.serebit.extensions.jda.hasPermissions
import com.serebit.extensions.jda.isNotBot
import com.serebit.extensions.jda.notInBlacklist
import com.serebit.extensions.toBooleanOrNull
import com.serebit.extensions.toCharOrNull
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.annotations.Command as CommandAnnotation

internal class Command(
    private val function: KFunction<Unit>,
    val name: String,
    description: String,
    private val access: Access,
    private val locale: Locale,
    private val splitLastParameter: Boolean = true,
    private val isHidden: Boolean,
    private val memberPermissions: List<Permission> = emptyList()
) {
    private val parameterTypes: List<KClass<out Any>> = function.valueParameters.map { it.type.jvmErasure }.drop(1)
    val isNotHidden get() = !isHidden
    val summary = "`$name ${parameterTypes.joinToString(" ") { "<${it.simpleName}>" }}`"
    val helpField = MessageEmbed.Field(summary, buildString {
        append("$description\n")
        append("Access: ${access.description}\n")
        append("Locale: ${locale.description}\n")
    }, false)

    operator fun invoke(instance: Module, evt: MessageReceivedEvent, parameters: List<Any>): Any? =
        function.call(instance, evt, *parameters.toTypedArray())

    internal fun looselyMatches(rawMessageContent: String): Boolean =
        rawMessageContent.split(" ")[0] == config.prefix + name

    internal fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? {
        if (evt.isInvalidCommandInvocation) return null
        val tokens = tokenizeMessage(evt.message.contentRaw)
        return when {
            tokens[0] != config.prefix + name -> null
            parameterTypes.size != tokens.size - 1 -> null
            else -> parseTokens(evt, tokens).let { parsedTokens ->
                if (parsedTokens.any { it == null }) null else parsedTokens.filterNotNull()
            }
        }
    }

    private fun tokenizeMessage(message: String): List<String> {
        val splitParameters = message.split(" ").filter(String::isNotBlank)
        return if (splitLastParameter) {
            splitParameters
        } else {
            splitParameters.slice(0 until parameterTypes.size) +
                splitParameters.drop(parameterTypes.size).joinToString(" ")
        }.filter(String::isNotBlank)
    }

    private fun parseTokens(evt: MessageReceivedEvent, tokens: List<String>): List<Any?> =
        parameterTypes.zip(tokens.drop(1)).map { (type, string) ->
            castParameter(evt, type, string)
        }

    @Suppress("ComplexMethod")
    private fun castParameter(
        evt: MessageReceivedEvent,
        type: KClass<out Any>,
        string: String
    ): Any? = when (type) {
        String::class -> string
        Int::class -> string.toIntOrNull()
        Long::class -> string.toLongOrNull()
        Double::class -> string.toDoubleOrNull()
        Float::class -> string.toFloatOrNull()
        Short::class -> string.toShortOrNull()
        Byte::class -> string.toByteOrNull()
        Boolean::class -> string.toBooleanOrNull()
        Char::class -> string.toCharOrNull()
        User::class -> evt.jda.getUserByMention(string)
        Member::class -> evt.guild.getMemberByMention(string)
        Channel::class -> evt.guild.getTextChannelByMention(string)
        Emote::class -> evt.jda.getEmoteByMention(string)?.asEmoji ?: Emote(string)
        else -> null
    }

    fun isInvokeableByAuthor(evt: MessageReceivedEvent): Boolean {
        val hasAccess = when (access) {
            Access.ALL -> true
            Access.GUILD_OWNER -> evt.member == evt.member?.guild?.owner
            Access.BOT_OWNER -> evt.author == evt.jda.asBot().applicationInfo.complete().owner
            Access.RANK_ABOVE -> evt.guild != null && evt.member.roles[0] > evt.guild.selfMember.roles[0]
            Access.RANK_SAME -> evt.guild != null && evt.member.roles[0] == evt.guild.selfMember.roles[0]
            Access.RANK_BELOW -> evt.guild != null && evt.member.roles[0] < evt.guild.selfMember.roles[0]
        }

        val correctLocale = when (locale) {
            Locale.ALL -> true
            Locale.GUILD -> evt.guild != null
            Locale.PRIVATE_CHANNEL -> evt.guild == null
        }

        return hasAccess && correctLocale
    }

    private val MessageReceivedEvent.isValidCommandInvocation: Boolean
        get() = if (author.isNotBot && author.notInBlacklist && member.hasPermissions(memberPermissions)) {
            isInvokeableByAuthor(this)
        } else false

    private val MessageReceivedEvent.isInvalidCommandInvocation: Boolean get() = !isValidCommandInvocation

    companion object {
        internal val validParameterTypes = setOf(
            Boolean::class,
            Byte::class,
            Short::class,
            Int::class,
            Long::class,
            Float::class,
            Double::class,
            User::class,
            Member::class,
            Channel::class,
            Emote::class,
            Char::class,
            String::class
        )
    }
}
