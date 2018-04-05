package com.serebit.autotitan.api

import net.dv8tion.jda.core.events.Event

interface FunctionWrapper {
    operator fun invoke(evt: Event, vararg args: Any)
}
