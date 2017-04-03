import listeners.MessageListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import java.io.File
import java.util.*

fun main(args: Array<String>) {
  val classpathFolder = File(ClassLoader.getSystemResource("").toURI()).parentFile
  val tokenFile = File(classpathFolder.path + "/token.txt")
  val tokenFileIsValid = tokenFile.exists() && tokenFile.readText() != ""
  var token = if (tokenFileIsValid) {
    if (args.isNotEmpty() && args[0] == "--quick") {
      getExistingToken(tokenFile)
    } else {
      getNewOrExistingToken(tokenFile)
    }
  } else {
    getNewToken()
  }
  val jda = JDABuilder(AccountType.BOT)
      .setToken(token)
      .buildBlocking()
  jda.addEventListener(MessageListener(">"))
  tokenFile.writeText(token)
}

fun getExistingToken(tokenFile: File): String {
  return tokenFile.readText()
}

fun getNewOrExistingToken(tokenFile: File): String {
  println("Enter new token, or newline to use saved token:")
  print(">")
  var input = Scanner(System.`in`).nextLine()
  return if (input == "") {
    tokenFile.readText()
  } else {
    input
  }
}

fun getNewToken(): String {
  println("Enter new token:")
  print(">")
  return Scanner(System.`in`).nextLine()
}