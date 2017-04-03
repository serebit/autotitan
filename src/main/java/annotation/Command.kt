package annotation

@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String = "", val noPm: Boolean = false)