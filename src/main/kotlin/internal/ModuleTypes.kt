package com.serebit.autotitan.internal

import com.serebit.autotitan.BotConfig
import com.serebit.autotitan.api.Access
import com.serebit.autotitan.api.CommandTemplate
import com.serebit.autotitan.api.GroupTemplate
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

internal interface HelpProvider {
    val helpField: MessageEmbed.Field
    val helpSignature: Regex
}

internal class Module(
    val name: String,
    val isOptional: Boolean,
    groupTemplates: List<GroupTemplate>,
    commandTemplates: List<CommandTemplate>,
    private val listeners: List<Listener>,
    val config: BotConfig
) {
    val allCommandsField get() = getCommandField(commands)
    val isStandard get() = !isOptional
    private val groups = groupTemplates.map { it.build() }
    private val commands = commandTemplates.map { it.build(null) }
    private val allCommands = commands + groups.map { it.commands }.flatten()
    private val allInvokeable = allCommands + groups

    fun getInvokeableCommandField(evt: MessageReceivedEvent) =
        getCommandField(commands.filter { it.access.matches(evt) && !it.isHidden })

    private fun getCommandField(includedCommands: List<Command>): MessageEmbed.Field? {
        val fieldValue = buildString {
            if (groups.isNotEmpty()) {
                appendLine("*Groups:* ${groups.joinToString { "`${it.name}`" }}")
            }
            if (includedCommands.isNotEmpty()) {
                append("*Commands:* ${includedCommands.joinToString { it.summary }}")
            }
        }
        return if (fieldValue.isNotEmpty()) MessageEmbed.Field(name, fieldValue, false) else null
    }

    suspend fun invoke(evt: GenericEvent) {
        listeners.forEach {
            when (it) {
                is Listener.Suspending -> it(evt)
                is Listener.Normal -> it(evt)
            }
        }
        if (evt is MessageReceivedEvent && evt.isCommandInvocation) {
            val rawContent = evt.message.contentRaw.removePrefix(config.prefix)
            allCommands
                .asSequence()
                .filter { it.access.matches(evt) }
                .mapNotNull { command ->
                    Parser.tokenize(rawContent, command.invocationSignature)?.let { command to it }
                }
                .mapNotNull { (command, tokens) ->
                    Parser.parseTokens(evt, tokens, command.tokenTypes)?.let { command to it }
                }
                .firstOrNull()?.let { (command, parameters) ->
                    when (command) {
                        is Command.Suspending -> command(evt, parameters)
                        is Command.Normal -> command(evt, parameters)
                    }
                }
        }
    }

    private val MessageReceivedEvent.isCommandInvocation
        get() = message.contentRaw.startsWith(config.prefix) && author.canInvokeCommands

    private val User.canInvokeCommands get() = !isBot && idLong !in config.blackList

    fun helpFieldsBySignature(signature: String): List<MessageEmbed.Field> =
        allInvokeable.filter { it.helpSignature.matches(signature) }.map { it.helpField }
}

internal class Group(
    val name: String, description: String,
    commandTemplates: List<CommandTemplate>
) : HelpProvider {
    override val helpSignature = "(\\Q$name\\E)".toRegex()
    val commands = commandTemplates.map { it.build(this) }
    override val helpField = MessageEmbed.Field(
        "`name`",
        "$description\n${commands.joinToString { it.summary }}",
        false
    )
}

internal sealed class Command(
    val name: String, description: String,
    val access: Access,
    parent: Group?,
    val tokenTypes: List<TokenType>
) : HelpProvider {
    val isHidden = access.hidden
    val summary = buildString {
        append("`")
        parent?.let { append("${it.name} ") }
        append("$name ${tokenTypes.joinToString(" ") { "<${it.name}>" }}".trimEnd())
        append("`")
    }

    override val helpField = MessageEmbed.Field(summary, "$description\n${access.description}", false)
    override val helpSignature = buildString {
        append("^")
        parent?.let { append("${it.helpSignature} ") }
        append("(\\Q$name\\E)".toRegex())
        append("$")
    }.toRegex()

    val invocationSignature = buildString {
        append("^")
        parent?.let { append("${it.helpSignature} ") }
        append("(\\Q$name\\E)".toRegex())
        tokenTypes.signature().let { if (it.isNotBlank()) append(" $it") }
        append("$")
    }.toRegex(RegexOption.DOT_MATCHES_ALL)

    class Normal(
        name: String, description: String,
        access: Access, parent: Group?,
        tokenTypes: List<TokenType>,
        private inline val function: (MessageReceivedEvent, List<Any>) -> Unit
    ) : Command(name, description, access, parent, tokenTypes) {
        operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>) = function(evt, parameters)
    }

    class Suspending(
        name: String, description: String,
        access: Access, parent: Group?,
        tokenTypes: List<TokenType>,
        private inline val function: suspend (MessageReceivedEvent, List<Any>) -> Unit
    ) : Command(name, description, access, parent, tokenTypes) {
        suspend operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>) = function(evt, parameters)
    }
}

internal sealed class Listener {
    class Normal(private inline val function: (GenericEvent) -> Unit) : Listener() {
        operator fun invoke(evt: GenericEvent) = function(evt)
    }

    class Suspending(private inline val function: suspend (GenericEvent) -> Unit) : Listener() {
        suspend operator fun invoke(evt: GenericEvent) = function(evt)
    }
}
