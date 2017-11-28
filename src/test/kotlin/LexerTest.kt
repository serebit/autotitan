import com.serebit.autotitan.Configuration
import com.serebit.autotitan.api.parser.Token
import com.serebit.autotitan.api.parser.TokenType
import com.serebit.autotitan.api.parser.tokenizeMessage
import com.serebit.autotitan.config
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import java.util.*

class LexerTest : StringSpec() {
    init {
        config = Configuration.generateDummy()
        "tests lexer parsing" {
            forAll(
                    Gen.default(),
                    Gen.default(),
                    StringGenerator()
            ) { f: Float, i: Int, s: String ->
                tokenizeMessage("!run $f $i \"$s\"") == listOf(
                        Token(TokenType.INVOCATION, "!run"),
                        Token(TokenType.FLOAT, f.toString()),
                        Token(TokenType.INTEGER, i.toString()),
                        Token(TokenType.STRING, "\"$s\"")
                )
            }
        }
    }
}

private class StringGenerator : Gen<String> {
    override fun generate(): String = nextPrintableString(Random().nextInt(99) + 1)
}