import kotlin.random.Random

module("Text", isOptional = true) {
    command("randomCase", "Randomizes the case of the input text.") { text: LongString ->
        channel.sendMessage(text.value.map {
            if (Random.nextBoolean()) it.toUpperCase() else it.toLowerCase()
        }.joinToString("")).queue()
    }

    command("reverse", "Reverses the input text.") { text: LongString ->
        channel.sendMessage(text.value.reversed()).queue()
    }
}
