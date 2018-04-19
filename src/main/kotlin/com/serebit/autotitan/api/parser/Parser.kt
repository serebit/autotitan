package com.serebit.autotitan.api.parser

import com.serebit.autotitan.data.Emote
import com.serebit.extensions.jda.getMemberByMention
import com.serebit.extensions.jda.getTextChannelByMention
import com.serebit.extensions.jda.getUserByMention
import com.serebit.extensions.toCharOrNull
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object Parser {
    fun castToken(evt: MessageReceivedEvent, type: KClass<out Any>, token: String): Any? = when {
        type.isSubclassOf(Number::class) -> castNumeral(type, token)
        type.isSubclassOf(IMentionable::class) -> castJdaEntity(evt, type, token)
        else -> castOther(evt, type, token)
    }

    private fun castJdaEntity(
        evt: MessageReceivedEvent,
        type: KClass<out Any>,
        token: String
    ): IMentionable? = when (type) {
        User::class -> evt.jda.getUserByMention(token)
        Member::class -> evt.guild.getMemberByMention(token)
        Channel::class -> evt.guild.getTextChannelByMention(token)
        else -> null
    }

    private fun castNumeral(
        type: KClass<out Any>,
        token: String
    ): Number? = when (type) {
        Int::class -> token.toIntOrNull()
        Long::class -> token.toLongOrNull()
        Double::class -> token.toDoubleOrNull()
        Float::class -> token.toFloatOrNull()
        Short::class -> token.toShortOrNull()
        Byte::class -> token.toByteOrNull()
        else -> null
    }

    private fun castOther(
        evt: MessageReceivedEvent,
        type: KClass<out Any>,
        token: String
    ): Any? = when (type) {
        String::class -> token
        Char::class -> token.toCharOrNull()
        Emote::class -> Emote.from(token, evt.jda)
        else -> null
    }
}
