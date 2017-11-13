package com.serebit.extensions.jda

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder

fun jda(accountType: AccountType, init: JDABuilder.() -> Unit): JDA {
    return JDABuilder(accountType).apply(init).buildBlocking()
}