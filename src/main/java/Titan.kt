import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import java.io.File
import java.util.*

fun main(args: Array<String>) {
  val classpathFolder = File(ClassLoader.getSystemResource("").toURI()).parentFile
  val newFile = File(classpathFolder.path + "/token.txt")
  var token: String
  if (newFile.exists()) {
    println("Enter new token, or newline to use saved token:")
    print(">")
    token = Scanner(System.`in`).nextLine()
    if (token == "") {
      token = newFile.readText()
    } else {
      newFile.writeText(token)
    }
  } else {
    println("Enter new token:")
    print(">")
    token = Scanner(System.`in`).nextLine()
    newFile.writeText(token)
  }

  val jda = JDABuilder(AccountType.BOT)
      .setToken(token)
      .buildBlocking()
  jda.addEventListener(MessageListener())
}

