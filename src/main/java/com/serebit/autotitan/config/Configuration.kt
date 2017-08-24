package com.serebit.autotitan.config

import com.google.gson.Gson
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.io.File
import java.util.*

object Configuration {
    private val parentFolder = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    private val file = File("$parentFolder/.config").apply { mkdirs() }
    var token: String
        internal set
    var prefix: String
        internal set
    val blackList: BlackList

    init {
        if (file.exists()) {
            val configData = Gson().fromJson(file.readText(), ConfigurationData::class.java)
            token = configData.token
            prefix = configData.prefix
            blackList = configData.blackList
        } else {
            token = prompt("Enter new token:")
            prefix = prompt("Enter new command prefix:")
            blackList = BlackList()
            serialize()
        }
    }

    internal fun serialize() {
        file.writeText(Gson().toJson(ConfigurationData(token, prefix, blackList)))
    }

    private fun prompt(text: String): String {
        print("$text\n> ")
        val input = Scanner(System.`in`).nextLine().trim()
        return if (input.isBlank()) {
            prompt(text)
        } else input
    }

    private data class ConfigurationData(
            val token: String,
            val prefix: String,
            val blackList: BlackList
    )
}

class BlackList internal constructor() {
    private val set = mutableSetOf<String>()

    val size get() = set.size

    fun contains(element: User) = element.id in set

    fun containsAll(elements: Collection<User>) = set.containsAll(elements.map(User::getId))

    fun isEmpty() = set.isEmpty()

    fun add(element: User) = set.add(element.id)

    fun addAll(elements: Collection<User>) = set.addAll(elements.map(User::getId))

    fun clear() = set.clear()

    fun iterator(jda: JDA) = Iterator(jda)

    fun remove(element: User) = set.remove(element.id)

    fun removeAll(elements: Collection<User>) = set.removeAll(elements.map(User::getId))

    fun retainAll(elements: Collection<User>) = set.retainAll(elements.map(User::getId))

    inner class Iterator(private val jda: JDA) : MutableIterator<User> {
        private val iterator = set.iterator()

        override fun hasNext() = iterator.hasNext()

        override fun next(): User = jda.getUserById(iterator.next())

        override fun remove() = iterator.remove()
    }
}

class GuildRoleMap internal constructor() {
    private val map = mutableMapOf<Long, Long?>()

    val size = map.size

    operator fun contains(key: Guild) = containsKey(key)

    fun containsKey(key: Guild) = map.containsKey(key.idLong)

    fun containsValue(value: Role) = map.containsValue(value.idLong)

    operator fun get(jda: JDA, key: Guild): Role? {
        val value = map[key.idLong]
        return if (value != null) jda.getRoleById(value) else null
    }

    fun isEmpty() = map.isEmpty()

    fun getEntries(jda: JDA) = map.entries.map {
        Entry(
                jda.getGuildById(it.key),
                if (it.value != null) jda.getRoleById(it.value as Long) else null
        )
    }

    fun getKeys(jda: JDA) = map.keys.map { jda.getGuildById(it) }
    fun getValues(jda: JDA) = map.values.map {
        if (it != null) jda.getRoleById(it) else null
    }

    fun clear() = map.clear()

    fun put(key: Guild, value: Role?) = map.put(key.idLong, value?.idLong)

    fun putAll(from: Map<out Guild, Role>) = map.putAll(from.map { Pair(it.key.idLong, it.value.idLong) })

    fun remove(key: Guild) = map.remove(key.idLong)

    inner class Entry(override val key: Guild, override var value: Role?) : MutableMap.MutableEntry<Guild, Role?> {
        override fun setValue(newValue: Role?): Role? {
            value = newValue
            map[key.idLong] = value?.idLong
            return value
        }
    }
}