package com.serebit.autotitan.data

class GuildResourceMap<K, V> : MutableMap<Long, MutableMap<K, V>> by mutableMapOf()

class GuildResourceList<E> : MutableMap<Long, MutableList<E>> by mutableMapOf()
