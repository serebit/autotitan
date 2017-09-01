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
    fun setAutoRole(evt: MessageReceivedEvent, roleName: String): Unit = evt.run {
        val role = guild.roles.lastOrNull { it.name == roleName }
        if (role != null) {
            channel.sendMessage("Set autorole to `$roleName`.").complete()
            map.put(guild, role)
            dataManager.write("rolemap.json", map)
        } else {
            channel.sendMessage("`$roleName` does not exist.").complete()
        }
    }

    @ListenerFunction
    fun giveRole(evt: GuildMemberJoinEvent): Unit = evt.run {
        if (map.contains(guild)) {
            guild.controller.addRolesToMember(member, map[jda, guild]).complete()
        }
    }
}

private class GuildRoleMap internal constructor() {
    private val map = mutableMapOf<Long, Long>()

    operator fun contains(key: Guild) = containsKey(key)

    fun containsKey(key: Guild) = map.containsKey(key.idLong)

    operator fun get(jda: JDA, key: Guild): Role? {
        val value = map[key.idLong]
        return if (value != null) jda.getRoleById(value) else null
    }

    fun put(key: Guild, value: Role) = map.put(key.idLong, value.idLong)
}