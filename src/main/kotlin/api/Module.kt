package com.serebit.autotitan.api

import com.serebit.autotitan.BotConfig
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parser.Parser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

internal class Module(
    val name: String,
    val isOptional: Boolean,
    groupTemplates: List<GroupTemplate>,
    commandTemplates: List<CommandTemplate>,
    private val listeners: List<Listener>,
    private val config: BotConfig
) : CoroutineScope {
    override val coroutineContext = Dispatchers.Default
    val commandListField
        get() = MessageEmbed.Field(name, commands.filter { !it.isHidden }.joinToString { it.summary }, false)
    val isStandard get() = !isOptional
    private val groups = groupTemplates.map { it.build() }
    private val commands = commandTemplates.map { it.build(null) }
    private val allCommands = commands + groups.map { it.commands }.flatten()
    private val allInvokeable = allCommands + groups

    fun getInvokeableCommandField(evt: MessageReceivedEvent): MessageEmbed.Field? {
        val validCommands = commands.filter { it.access.matches(evt) && !it.isHidden }
        return if (validCommands.isNotEmpty()) {
            val fieldValue = buildString {
                if (groups.isNotEmpty()) {
                    append("Groups: ${groups.joinToString(prefix = "`", postfix = "`") { it.name }}")
                }
                if (commands.isNotEmpty()) append("\nCommands: ${validCommands.joinToString { it.summary }}")
            }
            MessageEmbed.Field(name, fieldValue, false)
        } else null
    }

    fun invoke(evt: Event) = launch {
        listeners.asSequence()
            .filter { it.eventType.isInstance(evt) }
            .forEach { it(evt) }
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
                .firstOrNull()?.let { (command, parameters) -> command(evt, parameters) }
        }
    }

    fun helpFieldsBySignature(signature: String): List<MessageEmbed.Field> =
        allInvokeable.filter { it.helpSignature.matches(signature) }.map { it.helpField }

    private val MessageReceivedEvent.isCommandInvocation
        get() = message.contentRaw.startsWith(config.prefix) && author.canInvokeCommands

    private val User.canInvokeCommands get() = !isBot && idLong !in config.blackList
}

fun module(
    name: String,
    isOptional: Boolean = false,
    defaultAccess: Access = Access.All(),
    init: ModuleTemplate.() -> Unit
) = ModuleTemplate(name, isOptional, defaultAccess).apply(init)
