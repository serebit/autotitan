package com.serebit.autotitan.modules

import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.config
import kotlin.random.Random

@Suppress("UNUSED")
class Entertainment : ModuleTemplate(isOptional = true) {
    init {
        command("8", "Answers questions in 8-ball fashion.") { evt, _: LongString ->
            evt.channel.sendMessage(eightBallResponses.random()).queue()
        }

        command(
            "rate",
            "Rates the given thing on a scale of 0 to $ratingDenominator."
        ) { evt, thingToRate: LongString ->
            val seed = thingToRate.value.normalize()
                .hashCode()
                .plus(config.token.hashCode())
                .toLong()
            val rating = Random(seed).nextInt(ratingDenominator)
            evt.channel.sendMessage("I'd give $thingToRate a `$rating/$ratingDenominator`.").queue()
        }
    }

    private fun String.normalize(): String = this
        .toLowerCase()
        .filter { it.isLetterOrDigit() }

    companion object {
        private const val ratingDenominator = 10
        private val eightBallResponses = listOf(
            "It is certain.",
            "It is decidedly so.",
            "Without a doubt.",
            "Yes, definitely.",
            "You may rely on it.",
            "As I see it, yes.",
            "Most likely.",
            "Outlook good.",
            "Yes.",
            "Signs point to yes.",
            "Don't count on it.",
            "My reply is no.",
            "My sources say no.",
            "Outlook not so good.",
            "Very doubtful."
        )
    }
}
