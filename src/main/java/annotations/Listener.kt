package annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Listener(
    val name: String = "",
    val description: String = "",
    val serverOnly: Boolean = false
)
