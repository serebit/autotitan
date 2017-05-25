import java.io.File

object Singleton {
  val location = File(
      this::class.java.protectionDomain.codeSource.location.toURI()
  )
}