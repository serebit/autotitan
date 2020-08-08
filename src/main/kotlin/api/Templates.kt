package com.serebit.autotitan.api

import com.serebit.autotitan.BotConfig
import com.serebit.autotitan.internal.*
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.reflect.KType

sealed class InvokeableContainerTemplate {
    protected abstract val commands: MutableList<CommandTemplate>
    abstract val defaultAccess: Access

    fun addCommand(
        name: String, description: String, access: Access, parameterTypes: List<KType>,
        function: (MessageReceivedEvent, List<Any>) -> Unit
    ) {
        commands += CommandTemplate.Normal(name, description, access, parameterTypes, function)
    }

    fun addSuspendCommand(
        name: String, description: String, access: Access, parameterTypes: List<KType>,
        function: suspend (MessageReceivedEvent, List<Any>) -> Unit
    ) {
        commands += CommandTemplate.Suspending(name, description, access, parameterTypes, function)
    }
}

class ModuleTemplate(
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

    fun addListener(function: (GenericEvent) -> Unit) {
        listeners += Listener.Normal(function)
    }

    fun addSuspendListener(function: suspend (GenericEvent) -> Unit) {
        listeners += Listener.Suspending(function)
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

sealed class CommandTemplate(
    val name: String, val description: String,
    val access: Access,
    val parameterTypes: List<KType>
) {
    internal abstract fun build(parent: Group?): Command

    class Normal(
        name: String, description: String,
        access: Access,
        parameterTypes: List<KType>,
        private inline val function: (MessageReceivedEvent, List<Any>) -> Unit
    ) : CommandTemplate(name, description, access, parameterTypes) {
        private val tokenTypes = parameterTypes.map { TokenType.from(it) }.requireNoNulls()

        override fun build(parent: Group?) =
            Command.Normal(name.toLowerCase(), description, access, parent, tokenTypes, function)
    }

    class Suspending(
        name: String, description: String,
        access: Access,
        parameterTypes: List<KType>,
        private inline val function: suspend (MessageReceivedEvent, List<Any>) -> Unit
    ) : CommandTemplate(name, description, access, parameterTypes) {
        private val tokenTypes = parameterTypes.map { TokenType.from(it) }.requireNoNulls()

        override fun build(parent: Group?) =
            Command.Suspending(name.toLowerCase(), description, access, parent, tokenTypes, function)
    }
}
