@file:JvmName("GuildResourceCollections")

package com.serebit.autotitan.data

class GuildResourceMap<K, V> : MutableMap<Long, MutableMap<K, V>>
by (mutableMapOf<Long, MutableMap<K, V>>().withDefault { mutableMapOf() })

class GuildResourceList<E> : MutableMap<Long, MutableList<E>>
by (mutableMapOf<Long, MutableList<E>>().withDefault { mutableListOf() })
