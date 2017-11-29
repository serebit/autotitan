package com.serebit.autotitan.api.meta.annotations

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import net.dv8tion.jda.core.Permission

/**
 * Represents a bot command's attributes.
 *
 * @param name The string used to call the command from Discord. If left empty, uses the function
 * name instead.
 * @param description A short string describing the command and what it does.
 * @param locale The locale in which this command can be used.
 * @param access Who has access to this command.
 * @param hidden Defines whether or not this command will appear in help messages.
 * @param delimitFinalParameter If the last parameter of the command is a string, setting this to
 * false will prevent AutoTitan from splitting the string by whitespace.
 * @param memberPermissions The memberPermissions a Guild member must have in order to use the command.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Command(
        val name: String = "",
        val description: String = "",
        val locale: Locale = Locale.ALL,
        val access: Access = Access.ALL,
        val hidden: Boolean = false,
        val delimitFinalParameter: Boolean = true,
        val botPermissions: Array<Permission> = [],
        val memberPermissions: Array<Permission> = []
)
