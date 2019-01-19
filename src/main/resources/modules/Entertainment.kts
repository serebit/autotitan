import com.serebit.autotitan.api.command
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import kotlin.random.Random

fun String.normalize(): String = toLowerCase().filter { it.isLetterOrDigit() }

val ratingDenominator = 10
val eightBallResponses = listOf(
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

module("Entertainment", isOptional = true) {
    command("8", "Answers questions in 8-ball fashion.") { question: LongString ->
        channel.sendMessage("In response to $question: ${eightBallResponses.random()}").queue()
    }

    command("rate", "Rates the given thing on a scale of 0 to $ratingDenominator.") { thingToRate: LongString ->
        val seed = thingToRate.value.normalize()
            .hashCode()
            .plus(config.token.hashCode())
            .toLong()
        val rating = Random(seed).nextInt(ratingDenominator)
        channel.sendMessage("I'd give $thingToRate a `$rating/$ratingDenominator`.").queue()
    }
}
