import com.serebit.autotitan.api.LongString
import com.serebit.autotitan.api.command
import com.serebit.autotitan.api.optionalModule
import kotlin.random.Random
import kotlin.random.nextInt

optionalModule("Text") {
    command("randomCase", "Randomizes the case of the input text.") { text: LongString ->
        channel.sendMessage(text.value.map {
            if (Random.nextBoolean()) it.toUpperCase() else it.toLowerCase()
        }.joinToString("")).queue()
    }

    command("altCase", "Alternates the case of every other letter in the input text.") { text: LongString ->
        val offset = Random.nextInt(0..1)
        channel.sendMessage(text.value.mapIndexed { index, it ->
            if (index % 2 == offset) it.toUpperCase() else it.toLowerCase()
        }.joinToString("")).queue()
    }

    command("reverse", "Reverses the input text.") { text: LongString ->
        channel.sendMessage(text.value.reversed()).queue()
    }
}
