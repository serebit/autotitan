
import com.serebit.autotitan.api.*
import com.serebit.autotitan.extensions.sendEmbed
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.User

val usernameLengthRange = 2..32

module("Owner", defaultAccess = Access.BotOwner()) {
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
        jda.presence.activity = Activity.playing("${prefix}help")
        channel.sendMessage("Set prefix to `${config.prefix}`.").queue()
    }

    group("blacklist") {
        command("add", "Adds a user to the blacklist.") { user: User ->
            if (user.idLong in config.blackList) {
                channel.sendMessage("${user.name} is already in the blacklist.").queue()
                return@command
            }
            config.blackList.add(user.idLong)
            channel.sendMessage("Added ${user.name} to the blacklist.").queue()
            config.serialize()
        }

        command("remove", "Removes a user from the blacklist.") { user: User ->
            if (user.idLong in config.blackList) {
                config.blackList.remove(user.idLong)
                config.serialize()
                channel.sendMessage("Removed ${user.name} from the blacklist.").queue()
            } else channel.sendMessage("${user.name} is not in the blacklist.").queue()
        }

        command("list", "Sends a list of blacklisted users in an embed.") {
            if (config.blackList.isEmpty()) {
                channel.sendMessage("The blacklist is empty.").queue()
            } else {
                channel.sendEmbed {
                    addField("Blacklisted Users", config.blackList.joinToString("\n") {
                        jda.getUserById(it)!!.asMention
                    }, true)
                }.queue()
            }
        }
    }

    command("serverList", "Sends the list of servers that the bot is in.") {
        channel.sendEmbed {
            jda.guilds.forEach {
                addField(it.name, "Owner: ${it.owner!!.asMention}\nMembers: ${it.members.size}\n", true)
            }
        }.queue()
    }

    command("leaveServer", "Leaves the server in which the command is invoked.") {
        channel.sendMessage("Leaving the server.").complete()
        guild.leave().queue()
    }
}
