package com.serebit.autotitan.internal

import com.serebit.autotitan.api.Emote
import com.serebit.autotitan.api.LongString
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

internal object Parser {
    fun tokenize(message: String, signature: Regex): List<String>? =
        signature.find(message)?.groups?.mapNotNull { it?.value }?.drop(1)

    fun parseTokens(evt: MessageReceivedEvent, tokens: List<String>, tokenTypes: List<TokenType>): List<Any>? =
        if (tokens.isNotEmpty()) castTokens(evt, tokenTypes, tokens.takeLast(tokenTypes.size)) else null

    private fun castTokens(evt: MessageReceivedEvent, types: List<TokenType>, tokens: List<String>): List<Any>? {
        val castedTokens = types.zip(tokens).map { (type, token) ->
            when (type) {
                is TokenType.Number -> castNumeral(type, token)
                is TokenType.Jda -> castJdaMentionable(evt, type, token)
                is TokenType.Other -> castOther(evt, type, token)
            }
        }
        return if (null in castedTokens) null else castedTokens.requireNoNulls()
    }

    private fun castJdaMentionable(evt: MessageReceivedEvent, type: TokenType.Jda, token: String): IMentionable? =
        when (type) {
            TokenType.Jda.UserToken -> evt.jda.getUserByMention(token)
            TokenType.Jda.MemberToken -> evt.guild.getMemberByMention(token)
            TokenType.Jda.TextChannelToken -> evt.guild.getTextChannelByMention(token)
            TokenType.Jda.RoleToken -> evt.guild.getRoleByMention(token)
        }

    private fun castNumeral(type: TokenType.Number, token: String): Number? = when (type) {
        TokenType.Number.ByteToken -> token.toByteOrNull()
        TokenType.Number.ShortToken -> token.toShortOrNull()
        TokenType.Number.IntToken -> token.toIntOrNull()
        TokenType.Number.LongToken -> token.toLongOrNull()
        TokenType.Number.BigIntToken -> token.toBigIntegerOrNull()
        TokenType.Number.DoubleToken -> token.toDoubleOrNull()
        TokenType.Number.FloatToken -> token.toFloatOrNull()
        TokenType.Number.BigDecimalToken -> token.toBigDecimalOrNull()
    }

    private fun castOther(evt: MessageReceivedEvent, type: TokenType.Other, token: String): Any? = when (type) {
        TokenType.Other.StringToken -> if (token.any { it.isWhitespace() }) null else token
        TokenType.Other.LongStringToken -> LongString(token)
        TokenType.Other.CharToken -> token.singleOrNull()
        TokenType.Other.BooleanToken -> token.toBooleanOrNull()
        TokenType.Other.EmoteToken -> Emote.from(token, evt.guild)
    }

    private fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

    private fun Guild.getMemberByMention(mention: String): Member? =
        getMemberById(mention.removeSurrounding("<@", ">").removePrefix("!"))

    private fun JDA.getUserByMention(mention: String): User? =
        retrieveUserById(mention.removeSurrounding("<@", ">").removePrefix("!")).complete()

    private fun Guild.getTextChannelByMention(mention: String): TextChannel? =
        getTextChannelById(mention.removeSurrounding("<#", ">"))

    private fun Guild.getRoleByMention(mention: String): Role? =
        getRoleById(mention.removeSurrounding("<@&", ">"))
}

internal sealed class TokenType(val name: String, val signature: Regex) {
    sealed class Number(name: String, signature: Regex) : TokenType(name, signature) {
        object ByteToken : Number("Byte", "[+-]?\\d{1,3}?".toRegex())
        object ShortToken : Number("Short", "[+-]?\\d{1,5}?".toRegex())
        object IntToken : Number("Int", "[+-]?\\d{1,10}?".toRegex())
        object LongToken : Number("Long", "[+-]?\\d{1,19}?L?".toRegex())
        object BigIntToken : Number("BigInt", "[+-]?\\d+?".toRegex())
        object FloatToken : Number("Float", "[+-]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE]\\d+)?[fF]?".toRegex())
        object DoubleToken : Number("Double", "[+-]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE]\\d+)?".toRegex())
        object BigDecimalToken : Number("BigDecimal", "[+-]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE]\\d+)?".toRegex())

        companion object {
            val typeAssociations = mapOf(
                Byte::class to ByteToken,
                Short::class to ShortToken,
                Int::class to IntToken,
                Long::class to LongToken,
                BigInteger::class to BigIntToken,
                Float::class to FloatToken,
                Double::class to DoubleToken,
                BigDecimal::class to BigDecimalToken
            )
        }
    }

    sealed class Jda(name: String, signature: Regex) : TokenType(name, signature) {
        object UserToken : Jda("User", "<@!?\\d+>|\\d+".toRegex())
        object MemberToken : Jda("Member", "<@!?\\d+>|\\d+".toRegex())
        object TextChannelToken : Jda("Channel", "<#\\d+>|\\d+".toRegex())
        object RoleToken : Jda("Role", "<@&\\d+>|\\d+".toRegex())

        companion object {
            val typeAssociations = mapOf(
                User::class to UserToken,
                Member::class to MemberToken,
                TextChannel::class to TextChannelToken,
                MessageChannel::class to TextChannelToken,
                Role::class to RoleToken
            )
        }
    }

    sealed class Other(name: String, signature: Regex) : TokenType(name, signature) {
        object EmoteToken : Other("Emote", "<a?:\\S+:\\d+>|\\d+|[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+".toRegex())
        object StringToken : Other("String", "\\S+".toRegex())
        object LongStringToken : Other("LongString", ".+?".toRegex())
        object BooleanToken : Other("Boolean", "true|false".toRegex())
        object CharToken : Other("Char", "\\S".toRegex())

        companion object {
            val typeAssociations = mapOf(
                Emote::class to EmoteToken,
                String::class to StringToken,
                LongString::class to LongStringToken,
                Boolean::class to BooleanToken,
                Char::class to CharToken
            )
        }
    }

    companion object {
        fun from(type: KClass<out Any>): TokenType? = when (type) {
            in Number.typeAssociations -> Number.typeAssociations[type]
            in Jda.typeAssociations -> Jda.typeAssociations[type]
            in Other.typeAssociations -> Other.typeAssociations[type]
            else -> null
        }
    }
}

internal fun List<TokenType>.signature() = if (isEmpty()) "" else joinToString(" ") { "(${it.signature.pattern})" }
