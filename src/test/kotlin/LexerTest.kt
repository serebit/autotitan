import com.serebit.autotitan.api.parser.Token
import com.serebit.autotitan.api.parser.Tokens
import com.serebit.autotitan.api.parser.tokenizeMessage
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import java.util.*

class LexerTest : StringSpec() {
    init {
        "tests lexer parsing" {
            forAll(
                    Gen.default(),
                    Gen.default(),
                    StringGenerator()
            ) { f: Float, i: Int, s: String ->
                tokenizeMessage("!run $f $i \"$s\"") == listOf(
                        Token(Tokens.INVOCATION, "!run"),
                        Token(Tokens.FLOAT, f.toString()),
                        Token(Tokens.INTEGER, i.toString()),
                        Token(Tokens.STRING, "\"$s\"")
                )
            }
        }
    }
}

private class StringGenerator : Gen<String> {
    override fun generate(): String = nextPrintableString(Random().nextInt(99) + 1)
}