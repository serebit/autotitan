package annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String = "",
    val description: String = "",
    val delimitFinalParameter: Boolean = true,
    val serverOnly: Boolean = false
)
