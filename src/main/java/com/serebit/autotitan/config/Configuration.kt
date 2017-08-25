package com.serebit.autotitan.config

import com.google.gson.Gson
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.User
import java.io.File
import java.util.*

private data class ConfigurationData(
        val token: String,
        val prefix: String,
        val blackList: BlackList
)

object Configuration {
    private val parentFolder = File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    private val file = File("$parentFolder/.config")
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
        file.apply { createNewFile() }.writeText(Gson().toJson(ConfigurationData(token, prefix, blackList)))
    }

    private fun prompt(text: String): String {
        print("$text\n> ")
        val input = Scanner(System.`in`).nextLine().trim()
        return if (input.isBlank()) {
            prompt(text)
        } else input
    }
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
