package annotation

@Target(AnnotationTarget.FUNCTION)
annotation class Group(
    val name: String = "",
    val group: String = "",
    val serverOnly: Boolean = false
)
