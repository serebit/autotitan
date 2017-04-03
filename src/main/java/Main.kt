import com.google.gson.Gson
import listeners.MessageListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import java.io.File
import java.util.*

fun main(args: Array<String>) {
  val useExistingSettings = !(args.contains("-r") || args.contains("--reset"))
  val classpathFolder = File(ClassLoader.getSystemResource("").toURI()).parentFile
  val dataFile = File(classpathFolder.path + "/data/data.json")
  val botData: BotData
  if (useExistingSettings && dataFile.exists()) {
    botData = Gson().fromJson(dataFile.readText(), BotData::class.java)
  } else {
    botData = BotData(
        getNewToken(),
        getNewPrefix()
    )
  }
  val jda = JDABuilder(AccountType.BOT)
      .setToken(botData.token)
      .buildBlocking()
  jda.addEventListener(MessageListener(botData.prefix))
  dataFile.writeText(Gson().toJson(botData))
}

fun getNewToken(): String {
  print("Enter new token:\n>")
  return Scanner(System.`in`).nextLine()
}

fun getNewPrefix(): String {
  print("Enter new prefix:\n>")
  return Scanner(System.`in`).nextLine()
}
