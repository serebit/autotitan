import com.serebit.autotitan.api.extensions.sendEmbed
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.config
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.User
import oshi.SystemInfo
import java.lang.management.ManagementFactory
import java.time.Duration
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

val info get() = SystemInfo()
val percentFactor = 100
val metricBase = 1000f
val usernameLengthRange = 2..32

fun Long.asMetricUnit(unit: String, decimalPoints: Int = 1): String {
    val exponent = floor(log(toFloat() - 1, metricBase)).roundToInt()
    val unitPrefix = listOf("", "K", "M", "G", "T", "P", "E")[exponent]
    return "%.${decimalPoints}f $unitPrefix$unit".format(this / metricBase.pow(exponent))
}

fun Duration.toVerboseTimestamp(): String = buildString {
    append("${minusMinutes(toMinutes()).seconds}s")
    if (toMinutes() > 0L) append(" ${minusHours(toHours()).toMinutes()}m")
    if (toHours() > 0L) append(" ${minusDays(toDays()).toHours()}h")
    if (toDays() > 0L) append(" ${toDays()}d")
}

fun Long.asPercentageOf(total: Long): Long = this / total * percentFactor

module("Owner", defaultAccess = Access.BotOwner()) {
    command("systemInfo", "Gets information about the system that the bot is running on.") {
        val process = info.operatingSystem.getProcess(info.operatingSystem.processId)
        val processorModel = info.hardware.processor.name.replace("(\\(R\\)|\\(TM\\)|@ .+)".toRegex(), "")
        val processorCores = info.hardware.processor.physicalProcessorCount
        val processorFrequency = info.hardware.processor.vendorFreq
        val totalMemory = info.hardware.memory.total
        val usedMemory = info.hardware.memory.total - info.hardware.memory.available
        val processMemory = process.residentSetSize
        val processUptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().uptime).toVerboseTimestamp()
        channel.sendEmbed {
            addField(
                "Processor",
                """
                    Model: `$processorModel`
                    Cores: `$processorCores`
                    Frequency: `${processorFrequency.asMetricUnit("Hz")}`
                    """.trimIndent(),
                false
            )
            addField(
                "Memory",
                """
                    Total: `${totalMemory.asMetricUnit("B")}`
                    Used: `${usedMemory.asMetricUnit("B")} (${usedMemory.asPercentageOf(totalMemory)}%)`
                    Process: `${processMemory.asMetricUnit("B")} (${processMemory.asPercentageOf(totalMemory)}%)`
                    """.trimIndent(),
                false
            )
            addField(
                "Uptime",
                """
                    System: `${Duration.ofMillis(info.hardware.processor.systemUptime).toVerboseTimestamp()}`
                    Process: `$processUptime`
                    """.trimIndent(),
                false
            )
        }.queue()
    }

    command("setName", "Changes the bot's username.") { name: LongString ->
        if (name.value.length !in usernameLengthRange) {
            channel.sendMessage("Usernames must be between 2 and 32 characters in length.").queue()
        } else {
            jda.selfUser.manager.setName(name.value).queue()
            channel.sendMessage("Renamed to $name.").queue()
        }
    }

    command("setPrefix", "Changes the bot's command prefix.") { prefix: String ->
        if (prefix.isBlank() || prefix.contains("\\s".toRegex())) {
            channel.sendMessage("Invalid prefix. Prefix must not be empty, and may not contain whitespace.")
            return@command
        }
        config.prefix = prefix
        config.serialize()
        jda.presence.game = Game.playing("${prefix}help")
        channel.sendMessage("Set prefix to `${config.prefix}`.").queue()
    }

    command("blackListAdd", "Adds a user to the blacklist.") { user: User ->
        if (user.idLong in config.blackList) {
            channel.sendMessage("${user.name} is already in the blacklist.").queue()
            return@command
        }
        config.blackList.add(user.idLong)
        channel.sendMessage("Added ${user.name} to the blacklist.").queue()
        config.serialize()
    }

    command("blackListRemove", "Removes a user from the blacklist.") { user: User ->
        if (user.idLong in config.blackList) {
            config.blackList.remove(user.idLong)
            config.serialize()
            channel.sendMessage("Removed ${user.name} from the blacklist.").queue()

        } else channel.sendMessage("${user.name} is not in the blacklist.").queue()
    }

    command("blackList", "Sends a list of blacklisted users in an embed.") {
        if (config.blackList.isEmpty()) {
            channel.sendMessage("The blacklist is empty.").queue()
        } else {
            channel.sendEmbed {
                addField("Blacklisted Users", config.blackList.joinToString("\n") {
                    jda.getUserById(it).asMention
                }, true)
            }.queue()
        }
    }

    command("serverList", "Sends the list of servers that the bot is in.") {
        channel.sendEmbed {
            jda.guilds.forEach {
                addField(it.name, "Owner: ${it.owner.asMention}\nMembers: ${it.members.size}\n", true)
            }
        }.queue()
    }

    command("leaveServer", "Leaves the server in which the command is invoked.") {
        channel.sendMessage("Leaving the server.").complete()
        guild.leave().queue()
    }
}
