package com.serebit.autotitan.modules

import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.parameters.LongString
import java.util.*

@Suppress("UNUSED")
class Text : ModuleTemplate(isOptional = true) {
    private val random = Random()

    init {
        command("randomCase", "Randomizes the case of the input text.") { evt, text: LongString ->
            evt.channel.sendMessage(text.value.map {
                if (random.nextBoolean()) it.toUpperCase() else it.toLowerCase()
            }.joinToString("")).queue()
        }

        command("reverse", "Reverses the input text.") { evt, text: LongString ->
            evt.channel.sendMessage(text.value.reversed()).queue()
        }
    }
}
