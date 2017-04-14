import annotations.CommandFunction
import com.google.common.reflect.ClassPath
import com.google.gson.Gson
import config.Configuration
import data.Command
import listeners.MessageListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.File
import java.util.*

fun main(args: Array<String>) {
  val useExistingSettings = !(args.contains("-r") || args.contains("--reset"))
  val dataFile = File(Singleton.getParentDirectory().parent + "/data/data.json")
  val botData: Configuration
  if (useExistingSettings && dataFile.exists()) {
    botData = Gson().fromJson(dataFile.readText(), Configuration::class.java)
  } else {
    botData = Configuration(
        getNewToken(),
        getNewPrefix()
    )
  }
  val jda = JDABuilder(AccountType.BOT)
      .setToken(botData.token)
      .buildBlocking()
  jda.addEventListener(MessageListener(botData.prefix, loadCommands(getExtensions())))
  dataFile.parentFile.mkdirs()
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

fun getExtensions(): MutableList<Class<*>> {
  val cp = ClassPath.from(Thread.currentThread().contextClassLoader)
  return cp.getTopLevelClassesRecursive("extensions")
      .map { it.load() }
      .toMutableList()
}

fun loadCommands(classes: MutableList<Class<*>>): MutableList<Command> {
  val commands = mutableListOf<Command>()
  classes.map { extension ->
    extension.methods
        .filter { it.isAnnotationPresent(CommandFunction::class.java) }
        .filter { it.parameterTypes[0] == MessageReceivedEvent::class.java }
        .forEach { commands.add(Command(extension.newInstance(), it, it.getAnnotation(CommandFunction::class.java))) }
  }
  return commands
}

object Singleton {
  fun getParentDirectory(): File = File(this::class.java.protectionDomain.codeSource.location.toURI())
}
