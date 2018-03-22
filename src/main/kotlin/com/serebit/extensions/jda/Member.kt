@file:JvmName("MemberExtensions")

package com.serebit.extensions.jda

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member

fun Member?.hasPermissions(permissions: Collection<Permission>): Boolean =
    this?.hasPermission(permissions.toMutableList()) ?: false
