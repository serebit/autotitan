package annotation

/**
 * Created by gingerdeadshot on 4/1/17.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String)