@file:JvmName("EmoteExtensions")

package com.serebit.extensions.jda

import net.dv8tion.jda.core.entities.Emote

val Emote.asEmoji get() = com.serebit.autotitan.data.Emote(idLong)
