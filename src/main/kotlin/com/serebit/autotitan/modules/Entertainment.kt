package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.config
import com.serebit.extensions.randomEntry
import java.util.*
import kotlin.math.roundToInt

@Suppress("UNUSED")
class Entertainment : Module(isOptional = true) {
    private val deterministicRandom = Random()
    private val random = Random()

    init {
        command("8", "Answers questions in 8-ball fashion.", delimitLastString = false) { evt, _: String ->
            evt.channel.sendMessage(eightBallResponses.randomEntry()).complete()
        }

        command(
            "rate",
            "Rates the given thing on a scale of 0 to $ratingDenominator.",
            delimitLastString = false
        ) { evt, thingToRate: String ->
            deterministicRandom.setSeed(
                thingToRate.normalize()
                    .hashCode()
                    .plus(config.token.hashCode())
                    .toLong()
            )
            val rating = deterministicRandom.next(ratingDenominator)
            evt.channel.sendMessage("I'd give $thingToRate a `$rating/$ratingDenominator`.").complete()
        }
    }

    private fun String.normalize(): String = this
        .toLowerCase()
        .filter { it.isLetterOrDigit() }

    private fun Random.next(bound: Int) = (nextFloat() * bound).roundToInt()

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
