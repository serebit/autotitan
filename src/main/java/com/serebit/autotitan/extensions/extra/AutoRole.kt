package com.serebit.autotitan.extensions.extra

import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import com.serebit.autotitan.api.annotations.ListenerFunction
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@ExtensionClass
class AutoRole {
    private val map: GuildRoleMap
    private val dataManager = DataManager(this::class.java)

    init {
        map = dataManager.read("rolemap.json", GuildRoleMap::class.java) ?: GuildRoleMap()
        dataManager.write("rolemap.json", map)
    }

    @CommandFunction(
            permissions = arrayOf(Permission.MANAGE_ROLES),
            delimitFinalParameter = false
    )
    fun setAutoRole(evt: MessageReceivedEvent, roleName: String) {
        val role = evt.guild.roles.lastOrNull { it.name == roleName }
        if (role != null) {
            evt.channel.sendMessage("Set autorole to `$roleName`.").complete()
            map.put(evt.guild, role)
            dataManager.write("rolemap.json", map)
        } else {
            evt.channel.sendMessage("`$roleName` does not exist.")
        }
    }

    @ListenerFunction
    fun giveRole(evt: GuildMemberJoinEvent) {
        if (map.contains(evt.guild)) {
            evt.guild.controller.addRolesToMember(evt.member, map[evt.jda, evt.guild]).complete()
        }
    }
}

private class GuildRoleMap internal constructor() {
    private val map = mutableMapOf<Long, Long?>()

    val size get() = map.size

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