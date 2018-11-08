import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import kotlin.random.Random

module("Text", isOptional = true) {
    command("randomCase", "Randomizes the case of the input text.") { evt, text: LongString ->
        evt.channel.sendMessage(text.value.map {
            if (Random.nextBoolean()) it.toUpperCase() else it.toLowerCase()
        }.joinToString("")).queue()
    }

    command("reverse", "Reverses the input text.") { evt, text: LongString ->
        evt.channel.sendMessage(text.value.reversed()).queue()
    }
}
