package com.serebit.autotitan.api.parser

import com.serebit.autotitan.api.parameters.Emote
import com.serebit.autotitan.api.parameters.LongString
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

internal sealed class TokenType(val name: String, val signature: Regex) {
    sealed class NumberToken(name: String, signature: Regex) : TokenType(name, signature) {
        object ByteToken : NumberToken("Byte", "\\d+?".toRegex())
        object ShortToken : NumberToken("Short", "\\d+?".toRegex())
        object IntToken : NumberToken("Int", "\\d+?".toRegex())
        object LongToken : NumberToken("Long", "\\d+?L?".toRegex())
        object BigIntToken : NumberToken("BigInt", "\\d+?".toRegex())
        object FloatToken : NumberToken("Float", "\\d+?.\\d+?f?".toRegex())
        object DoubleToken : NumberToken("Double", "\\d+?.\\d+?".toRegex())
        object BigDecimalToken : NumberToken("BigDecimal", "\\d+?.\\d+?".toRegex())

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

    sealed class JdaToken(name: String, signature: Regex) : TokenType(name, signature) {
        object UserToken : JdaToken("User", "<?@?!?\\d+?>?".toRegex())
        object MemberToken : JdaToken("Member", "<?@?!?\\d+?>?".toRegex())
        object TextChannelToken : JdaToken("Channel", "<?#?!?\\d+?>?".toRegex())
        object RoleToken : JdaToken("Role", "(?:<@&)?\\d+?>?".toRegex())

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

    sealed class OtherToken(name: String, signature: Regex) : TokenType(name, signature) {
        object EmoteToken : OtherToken("Emote", "<?a?:\\S+?:\\d+?>|[^A-Za-z\\d\\s]+?".toRegex())
        object StringToken : OtherToken("String", "\\S+?".toRegex())
        object LongStringToken : OtherToken("LongString", ".+?".toRegex())
        object BooleanToken : OtherToken("Boolean", "true|false".toRegex())
        object CharToken : OtherToken("Char", "\\S".toRegex())

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
            in NumberToken.typeAssociations -> NumberToken.typeAssociations[type]
            in JdaToken.typeAssociations -> JdaToken.typeAssociations[type]
            in OtherToken.typeAssociations -> OtherToken.typeAssociations[type]
            else -> null
        }
    }
}

internal fun List<TokenType>.signature() = if (isEmpty()) "" else joinToString(" ", "(", ")") { it.signature.pattern }
