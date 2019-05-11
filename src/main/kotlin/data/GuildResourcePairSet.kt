package com.serebit.autotitan.data

import net.dv8tion.jda.core.entities.Guild

class GuildResourcePairSet<K : Any, V : Any> : MutableMap<GuildId, MutableSet<Pair<K, V>>> by mutableMapOf() {
    operator fun contains(guild: Guild): Boolean = guild.idLong in this

    operator fun get(guild: Guild) = this.getOrPut(guild.idLong) { mutableSetOf() }

    operator fun get(guild: Guild, key: K): Set<V> =
        this[guild].filter { it.first == key }.map { it.second }.toSet()

    fun add(guild: Guild, pair: Pair<K, V>): Boolean = this[guild].add(pair)

    fun remove(guild: Guild, pair: Pair<K, V>) = this[guild].removeIf { it == pair }

    fun removeAll(guild: Guild, key: K) = this[guild.idLong]?.removeAll { it.first == key }
}
