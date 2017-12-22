package com.serebit.autotitan.modules.standard

import com.serebit.autotitan.api.Module
import com.serebit.extensions.jda.embed
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import oshi.SystemInfo
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow

private val dateFormat = DateTimeFormatter.ofPattern("d MMM, yyyy")

class General : Module() {
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

    init {
        command("ping") { evt ->
            evt.channel.sendMessage("Pong. The last ping was ${evt.jda.ping}ms.").complete()
        }

        command("systemInfo") { evt ->
            evt.channel.sendEmbed {
                setColor(evt.guild?.getMember(evt.jda.selfUser)?.color)
                systemInfo.forEach { key, value ->
                    addField(key, value, true)
                }
            }.complete()
        }

        command("serverInfo") { evt ->
            evt.run {
                val onlineMemberCount = guild.members.count { it.onlineStatus != OnlineStatus.OFFLINE }
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
                    addField("Online Members", onlineMemberCount.toString(), true)
                    addField("Total Members", guild.members.size.toString(), true)
                    addField("Bots", guild.members.count { it.user.isBot }.toString(), true)
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

        command("selfInfo") { evt ->
            evt.channel.sendMessage(evt.member.info).complete()
        }

        command("memberInfo") { evt, member: Member ->
            evt.channel.sendMessage(member.info).complete()
        }
    }
}

private val Member.info: MessageEmbed
    get() {
        val title = "${user.name}#${user.discriminator}" + if (nickname != null) {
            " ($nickname)"
        } else {
            ""
        }
        val status = onlineStatus.name
                .toLowerCase()
                .replace("_", " ")
                .capitalize()
                .plus(if (game != null) " - Playing ${game?.name}" else "")
        val roles = if (roles.isNotEmpty()) {
            roles.joinToString(", ") { it.name }
        } else null

        return embed {
            setTitle(title, null)
            setDescription(status)
            setColor(color)
            setThumbnail(user.effectiveAvatarUrl)
            addField("Joined Discord", user.creationTime.format(dateFormat), true)
            addField("Joined Server", joinDate.format(dateFormat), true)
            addField("Do they own the server?", isOwner.asYesNo.capitalize(), true)
            addField("Are they a bot?", user.isBot.asYesNo.capitalize(), true)
            if (roles != null) addField("Roles", roles, true)
            setFooter("User ID: ${user.id}", null)
        }
    }

private val Long.asHumanReadableByteCount: String
    get() {
        val exponent = ceil(log(this.toDouble(), 1000.0)).toInt() - 1
        val unit = listOf("B", "kB", "MB", "GB", "TB", "PB", "EB")[exponent]
        return "%.1f $unit".format(this / 1000.0.pow(exponent))
    }

private val Boolean.asYesNo get() = if (this) "yes" else "no"
