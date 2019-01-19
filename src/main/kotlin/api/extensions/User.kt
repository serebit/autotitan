@file:JvmName("UserExtensions")

package api.extensions

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.User

private val ownerMap = mutableMapOf<JDA, User>()

val User.isBotOwner
    get() = ownerMap.getOrPut(jda) {
        jda.asBot().applicationInfo.complete().owner
    } == this
