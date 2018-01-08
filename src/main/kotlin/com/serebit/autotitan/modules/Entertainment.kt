package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.annotations.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*
import kotlin.math.roundToInt

class Entertainment : Module(isOptional = true) {
    private val deterministicRandom = Random()
    private val random = Random()
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

    @Command(
            name = "magic8",
            description = "A deterministic version of the 8-ball command.",
            hidden = true,
            splitLastParameter = false
    )
    fun deterministicEightBall(evt: MessageReceivedEvent, question: String) {
        deterministicRandom.setSeed(question.normalize()
                .hashCode()
                .toLong()
        )
        val responseIndex = deterministicRandom.nextInt(eightBallResponses.size - 1)
        evt.channel.sendMessage(eightBallResponses[responseIndex]).complete()
    }

    @Command(name = "8", description = "Answers questions in 8-ball fashion.", splitLastParameter = false)
    fun eightBall(evt: MessageReceivedEvent, @Suppress("UNUSED_PARAMETER") question: String) {
        val responseIndex = random.next(eightBallResponses.size - 1)
        evt.channel.sendMessage(eightBallResponses[responseIndex]).complete()
    }

    @Command(description = "Rates the given thing on a scale of 0 to 10.", splitLastParameter = false)
    fun rate(evt: MessageReceivedEvent, thingToRate: String) {
        deterministicRandom.setSeed(thingToRate.normalize().hashCode().toLong())
        val rating = deterministicRandom.next(10)
        evt.channel.sendMessage("I'd give $thingToRate a `$rating/10`.").complete()
    }

    private fun String.normalize(): String = this
            .toLowerCase()
            .filter { it.isLetterOrDigit() }

    private fun Random.next(bound: Int = 10) = (nextFloat() * bound).roundToInt()
}