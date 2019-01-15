package com.serebit.autotitan.api.parser

import com.serebit.autotitan.api.parameters.Emote
import com.serebit.autotitan.api.parameters.LongString
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal sealed class TokenType(val name: String, val signature: Regex) {
    sealed class NumberToken(name: String, signature: Regex) : TokenType(name, signature) {
        object ByteToken : NumberToken("Byte", "\\d+?".toRegex())
        object ShortToken : NumberToken("Short", "\\d+?".toRegex())
        object IntToken : NumberToken("Int", "\\d+?".toRegex())
        object LongToken : NumberToken("Long", "\\d+?L?".toRegex())
        object FloatToken : NumberToken("Float", "\\d+?.\\d+?f?".toRegex())
        object DoubleToken : NumberToken("Double", "\\d+?.\\d+?".toRegex())

        companion object {
            fun from(type: KClass<out Any>): NumberToken? = when (type) {
                Byte::class -> ByteToken
                Short::class -> ShortToken
                Int::class -> IntToken
                Long::class -> LongToken
                Float::class -> FloatToken
                Double::class -> DoubleToken
                else -> null
            }
        }
    }

    sealed class JdaToken(name: String, signature: Regex) : TokenType(name, signature) {
        object UserToken : JdaToken("User", "<?@?!?\\d+?>?".toRegex())
        object MemberToken : JdaToken("Member", "<?@?!?\\d+?>?".toRegex())
        object ChannelToken : JdaToken("Channel", "<?#?!?\\d+?>?".toRegex())

        companion object {
            fun from(type: KClass<out Any>): JdaToken? = when {
                type == User::class -> UserToken
                type == Member::class -> MemberToken
                type.isSubclassOf(Channel::class) -> ChannelToken
                else -> null
            }
        }
    }

    sealed class OtherToken(name: String, signature: Regex) : TokenType(name, signature) {
        object EmoteToken : OtherToken("Emote", "<?a?:\\S+?:\\d+?>|[^A-Za-z\\d\\s]+?".toRegex())
        object StringToken : OtherToken("String", "\\S+?".toRegex())
        object LongStringToken : OtherToken("LongString", ".+?".toRegex())
        object BooleanToken : OtherToken("Boolean", "true|false".toRegex())
        object CharToken : OtherToken("Char", "\\S".toRegex())

        companion object {
            fun from(type: KClass<out Any>): OtherToken? = when (type) {
                Emote::class -> EmoteToken
                String::class -> StringToken
                LongString::class -> LongStringToken
                Boolean::class -> BooleanToken
                Char::class -> CharToken
                else -> null
            }
        }
    }

    companion object {
        fun from(type: KClass<out Any>): TokenType? = when {
            type.isSubclassOf(Number::class) -> NumberToken.from(type)
            type.isSubclassOf(IMentionable::class) -> JdaToken.from(type)
            else -> OtherToken.from(type)
        }
    }
}

internal fun List<TokenType>.signature() = if (isEmpty()) "" else joinToString(" ", "(", ")") { it.signature.pattern }
