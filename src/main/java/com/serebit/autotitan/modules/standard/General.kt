package com.serebit.autotitan.modules.standard

import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import oshi.SystemInfo
import java.time.format.DateTimeFormatter

@ExtensionClass
class General {
    private val dateFormat = DateTimeFormatter.ofPattern("d MMM, yyyy")
    private val systemInfo by lazy {
        SystemInfo().run {
            val formatKeyValuePair = { it: Map.Entry<String, String> ->
                "${it.key}: `${it.value}`"
            }
            val format = { it: String ->
                it.replace('_', ' ')
                        .replace("(R)", "\u00AE")
                        .replace("(TM)", "\u2122")
            }

            mapOf(
                    "Hardware" to mapOf(
                            "Processor" to format(hardware.processor.name),
                            "Motherboard" to format(hardware.computerSystem.baseboard.model),
                            "Disk" to format(hardware.diskStores[0].model),
                            "Total Memory" to hardware.memory.total.asHumanReadableByteCount
                    ).asIterable().joinToString("\n", transform = formatKeyValuePair),
                    "Operating System" to mapOf(
                            "Name" to operatingSystem.family,
                            "Version" to operatingSystem.version.toString()
                    ).asIterable().joinToString("\n", transform = formatKeyValuePair),
                    "Java" to mapOf(
                            "Vendor" to System.getProperty("java.vendor"),
                            "Version" to System.getProperty("java.version")
                    ).asIterable().joinToString("\n", transform = formatKeyValuePair)
            )
        }
    }

    @CommandFunction(description = "Pings the bot.")
    fun ping(evt: MessageReceivedEvent): Unit = evt.run {
        channel.sendMessage("Pong. The last ping was ${jda.ping}ms.").complete()
    }

    @CommandFunction(description = "Gets information about the system that the bot is running on.")
    fun systemInfo(evt: MessageReceivedEvent) {
        evt.run {
            channel.sendEmbed {
                setColor(guild?.getMember(jda.selfUser)?.color)
                systemInfo.forEach { key, value ->
                    addField(key, value, true)
                }
            }.complete()
        }
    }

    @CommandFunction(description = "Gets information about the server.", locale = Locale.GUILD)
    fun serverInfo(evt: MessageReceivedEvent) {
        evt.run {
            val onlineMemberCount = guild.members.count { it.onlineStatus != OnlineStatus.OFFLINE }.toString()
            val hoistedRoles = guild.roles
                    .filter { it.name != "@everyone" && it.isHoisted }
                    .joinToString(", ") { it.name }

            channel.sendEmbed {
                setTitle(guild.name, null)
                setDescription("Created on ${guild.creationTime.format(dateFormat)}")
                setThumbnail(guild.iconUrl)
                setColor(guild.owner.color)
                addField("Owner", guild.owner.asMention, true)
                addField("Region", guild.region.toString(), true)
                addField("Online Members", onlineMemberCount, true)
                addField("Total Members", guild.members.size.toString(), true)
                addField("Text Channels", guild.textChannels.size.toString(), true)
                addField("Voice Channels", guild.voiceChannels.size.toString(), true)
                addField("Hoisted Roles", hoistedRoles, true)
                if (guild.selfMember.hasPermission(Permission.MANAGE_SERVER)) {
                    val permanentInvites = guild.invites.complete().filter { !it.isTemporary }
                    if (permanentInvites.isNotEmpty()) addField(
                            "Invite Link",
                            permanentInvites.first().url,
                            false
                    )
                }
                setFooter("Server ID: ${guild.id}", null)
            }.complete()
        }
    }

    @CommandFunction(description = "Gets information about the invoker.", locale = Locale.GUILD)
    fun selfInfo(evt: MessageReceivedEvent) = memberInfo(evt, evt.member)

    @CommandFunction(description = "Gets information about a specific server member.", locale = Locale.GUILD)
    fun memberInfo(evt: MessageReceivedEvent, member: Member) {
        evt.run {
            val title = "${member.user.name}#${member.user.discriminator}" + if (member.nickname != null) {
                " (${member.nickname})"
            } else {
                ""
            }
            val status = member.onlineStatus.name
                    .toLowerCase()
                    .replace("_", " ")
                    .capitalize()
                    .plus(if (member.game != null) " - Playing ${member.game?.name}" else "")
            val roles = if (member.roles.isNotEmpty()) {
                member.roles.joinToString(", ") { it.name }
            } else null

            channel.sendEmbed {
                setTitle(title, null)
                setDescription(status)
                setColor(member.color)
                setThumbnail(member.user.effectiveAvatarUrl)
                addField("Joined Discord", member.user.creationTime.format(dateFormat), true)
                addField("Joined this Server", member.joinDate.format(dateFormat), true)
                if (roles != null) addField("Roles", roles, true)
                addField("Do they own the server?", member.isOwner.asYesNo.capitalize(), true)
                addField("Are they a bot?", member.user.isBot.asYesNo.capitalize(), true)
                setFooter("User ID: ${member.user.id}", null)
            }.complete()
        }
    }
}

private val Long.asHumanReadableByteCount: String
    get() {
        val exponent = (Math.log(toDouble()) / 6.908).toInt()
        val unit = listOf("B", "kB", "MB", "GB", "TB", "PB", "EB")[exponent]
        return "%.1f $unit".format(this / Math.pow(1000.0, exponent.toDouble()))
    }

private val Boolean.asYesNo get() = if (this) "yes" else "no"
