package com.serebit.autotitan.api

import com.serebit.autotitan.BotConfig
import com.serebit.autotitan.internal.*
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

sealed class InvokeableContainerTemplate {
    protected abstract val commands: MutableList<CommandTemplate>
    abstract val defaultAccess: Access

    fun addCommand(template: CommandTemplate) {
        template.parameterTypes.map { TokenType.from(it) }.requireNoNulls()
        require(template.name.isNotBlank())
        commands += template
    }
}

data class ModuleTemplate(
    val name: String,
    val isOptional: Boolean = false,
    override inline val defaultAccess: Access
) : InvokeableContainerTemplate() {
    lateinit var config: BotConfig
        private set
    val dataManager = DataManager(name)
    private val groups = mutableListOf<GroupTemplate>()
    private val listeners = mutableListOf<Listener>()
    override val commands = mutableListOf<CommandTemplate>()

    fun addGroup(template: GroupTemplate) {
        require(template.name.isNotBlank())
        groups += template
    }

    fun <T : GenericEvent> addListener(eventType: KClass<T>, function: suspend (GenericEvent) -> Unit) {
        listeners += Listener(eventType, function)
    }

    internal fun build(config: BotConfig): Module {
        this.config = config
        return Module(name, isOptional, groups, commands, listeners, config)
    }
}

data class GroupTemplate(
    val name: String,
    val description: String = "",
    override inline val defaultAccess: Access
) : InvokeableContainerTemplate() {
    override val commands = mutableListOf<CommandTemplate>()

    internal fun build() = Group(name.toLowerCase(), description, commands)
}

data class CommandTemplate(
    val name: String, val description: String,
    val access: Access,
    val parameterTypes: List<KClass<out Any>>,
    val function: suspend (MessageReceivedEvent, List<Any>) -> Unit
) {
    private val tokenTypes = parameterTypes.map { TokenType.from(it) }.requireNoNulls()

    internal fun build(parent: Group?) = Command(name.toLowerCase(), description, access, parent, tokenTypes, function)
}
