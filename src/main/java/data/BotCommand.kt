package data

import java.lang.reflect.Method

data class BotCommand(
    val instance: Any,
    val method: Method
)