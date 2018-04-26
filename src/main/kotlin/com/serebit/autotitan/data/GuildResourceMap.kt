@file:JvmName("GuildResourceCollections")

package com.serebit.autotitan.data

import net.dv8tion.jda.core.entities.Guild

private typealias GuildId = Long

class GuildResourceMap<K, V> : MutableMap<GuildId, MutableMap<K, V>> by mutableMapOf() {
    operator fun contains(guild: Guild): Boolean = guild.idLong in this

    operator fun get(guild: Guild): MutableMap<K, V> = this.getOrPut(guild.idLong) { mutableMapOf() }
}
