import com.serebit.autotitan.api.parser.Token
import com.serebit.autotitan.api.parser.Tokens
import com.serebit.autotitan.api.parser.tokenizeMessage
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class LexerTest : StringSpec() {
    init {
        "check token" {
            tokenizeMessage("!run 2 \"test string\"") shouldBe listOf(
                    Token(Tokens.INVOCATION, "!run"),
                    Token(Tokens.INTEGER, "2"),
                    Token(Tokens.STRING, "\"test string\"")
            )
        }
    }
}