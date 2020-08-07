package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.ModuleCompanion
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.config
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*
import kotlin.math.roundToInt

@Suppress("UNUSED")
class Entertainment : Module(isOptional = true) {
    private val deterministicRandom = Random()

    @Suppress("UNUSED_PARAMETER")
    @Command(name = "8", description = "Answers questions in 8-ball fashion.", splitLastParameter = false)
    fun eightBall(evt: MessageReceivedEvent, question: String) {
        evt.channel.sendMessage(eightBallResponses.random()).complete()
    }

    @Command(
        description = "Rates the given thing on a scale of 0 to $defaultRatingDenominator.",
        splitLastParameter = false
    )
    fun rate(evt: MessageReceivedEvent, thingToRate: String) {
        deterministicRandom.setSeed(
            thingToRate.normalize()
                .hashCode()
                .toLong()
                .plus(config.token.hashCode())
        )
        val rating = deterministicRandom.next(defaultRatingDenominator)
        evt.channel.sendMessage("I'd give $thingToRate a `$rating/$defaultRatingDenominator`.").complete()
    }

    private fun String.normalize(): String = this
        .toLowerCase()
        .filter { it.isLetterOrDigit() }

    private fun Random.next(bound: Int) = (nextFloat() * bound).roundToInt()

    companion object : ModuleCompanion {
        private const val defaultRatingDenominator = 10
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

        override fun provide() = Entertainment()
    }
}
