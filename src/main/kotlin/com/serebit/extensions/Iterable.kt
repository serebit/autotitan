@file:JvmName("IterableExtensions")

package com.serebit.extensions

inline fun <T> Iterable<T>.chunkedBy(
    size: Int,
    maxChunkSize: Int = Int.MAX_VALUE,
    transform: (T) -> Int
): List<List<T>> {
    val zipped = toList().zip(toMutableList().map(transform))
    val list = mutableListOf(mutableListOf<T>())
    var accumulator = 0
    zipped.forEach { (item, itemSize) ->
        when {
            accumulator + itemSize <= size && list.last().size < maxChunkSize -> {
                accumulator += itemSize
                list.last().add(item)
            }
            itemSize <= size -> {
                accumulator = itemSize
                list.add(mutableListOf(item))
            }
            else -> {
                accumulator = 0
                list.add(mutableListOf())
            }
        }
    }
    return list.toList()
}
