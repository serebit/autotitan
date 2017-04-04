package annotation

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String = "",
    val group: Boolean = false,
    val parent: String = "",
    val delimitFinalParameter: Boolean = true,
    val serverOnly: Boolean = false
)
