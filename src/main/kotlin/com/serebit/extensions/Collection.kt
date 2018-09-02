@file:JvmName("CollectionExtensions")

package com.serebit.extensions

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
