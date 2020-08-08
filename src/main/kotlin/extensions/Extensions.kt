package com.serebit.autotitan.extensions

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.restaction.MessageAction

inline fun jda(token: String, init: JDABuilder.() -> Unit): JDA =
    JDABuilder.create(token, GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)).apply(init).build().awaitReady()

inline fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): MessageAction =
    sendMessage(EmbedBuilder().apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
        init()
    }.build())

fun String.limitLengthTo(max: Int): String = if (length > max) {
    substring(0 until max - 1) + '\u2026'
} else this

fun <T> Iterable<T>.chunkedBy(
    chunkSize: Int,
    maxChunks: Int = Int.MAX_VALUE,
    transform: (T) -> Int
): List<List<T>> = zip(map(transform)).fold(Accumulator<T>(chunkSize, maxChunks)) { acc, (item, itemSize) ->
    acc.accumulate(item, itemSize)
}.chunks

private data class Accumulator<T>(
    val maxChunkSize: Int,
    val maxChunks: Int,
    val chunks: MutableList<MutableList<T>> = mutableListOf(mutableListOf()),
    var chunkSizeAccumulator: Int = 0
) {
    fun accumulate(item: T, itemSize: Int) = when {
        chunkSizeAccumulator + itemSize <= maxChunkSize && chunks.last().size < maxChunks -> addToChunk(item, itemSize)
        itemSize <= maxChunkSize -> newChunkOf(item, itemSize)
        else -> newChunk()
    }

    private fun addToChunk(item: T, itemSize: Int) = apply {
        chunkSizeAccumulator += itemSize
        chunks.last().add(item)
    }

    private fun newChunkOf(item: T, itemSize: Int) = apply {
        chunkSizeAccumulator = itemSize
        chunks.add(mutableListOf(item))
    }

    private fun newChunk() = apply {
        chunkSizeAccumulator = 0
        chunks.add(mutableListOf())
    }
}

private val ownerMap = mutableMapOf<JDA, User>()

val User.isBotOwner
    get() = this == ownerMap.getOrPut(jda) {
        jda.retrieveApplicationInfo().complete().owner
    }
