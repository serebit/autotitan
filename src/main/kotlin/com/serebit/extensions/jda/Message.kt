@file:JvmName("MessageExtensions")

package com.serebit.extensions.jda

import net.dv8tion.jda.core.entities.Message

val Message.mentionsUsers get() = mentionedUsers.isNotEmpty() || mentionedMembers.isNotEmpty() || mentionsEveryone()
