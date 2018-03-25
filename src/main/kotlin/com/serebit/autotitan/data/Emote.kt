package com.serebit.autotitan.data

class Emote {
    val unicodeValue: String?
    val emoteIdValue: EmoteId?
    val isDiscordEmote get() = emoteIdValue != null
    val isUnicodeEmote get() = unicodeValue != null

    constructor(unicode: String) {
        unicodeValue = unicode
        emoteIdValue = null
    }

    constructor(emoteId: EmoteId) {
        unicodeValue = null
        emoteIdValue = emoteId
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Emote?) {
            other?.unicodeValue == unicodeValue && other?.emoteIdValue == emoteIdValue
        } else false
    }
}
