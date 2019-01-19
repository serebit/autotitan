package com.serebit.autotitan.api.parser

import com.serebit.autotitan.api.parameters.Emote
import com.serebit.autotitan.api.parameters.LongString
import net.dv8tion.jda.core.entities.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

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
