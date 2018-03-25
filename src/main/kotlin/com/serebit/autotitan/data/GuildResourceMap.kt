package com.serebit.autotitan.data

import net.dv8tion.jda.core.entities.Guild

class GuildResourceMap<K, V> : MutableMap<GuildId, MutableMap<K, V>> by mutableMapOf() {
    operator fun contains(guild: Guild): Boolean = guild.idLong in this

    operator fun get(guild: Guild): MutableMap<K, V> = this.getOrPut(guild.idLong) { mutableMapOf() }

    operator fun get(guild: Guild, key: K): V? = this[guild][key]

    fun put(guild: Guild, key: K, value: V): V? = this[guild.idLong]?.put(key, value)

    fun remove(guild: Guild, key: K) = this[guild.idLong]?.remove(key)
}
