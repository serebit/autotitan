import java.lang.reflect.Method

data class CommandData(
    val instance: Any
    val method: Method
)