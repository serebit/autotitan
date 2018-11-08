package com.serebit.autotitan.data

import com.serebit.logkat.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.ScriptEngineManager
import kotlin.coroutines.CoroutineContext

internal class ModuleLoader : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    suspend fun loadModules() {
        val factory = ScriptEngineManager().getEngineByExtension("kts").factory
        loadScripts().map {
            launch {
                factory!!.scriptEngine.eval(it.readText())
                Logger.debug("Loaded module from file ${it.name}.")
            }
        }.joinAll()
    }

    private fun loadScripts() = listOf(loadInternalScripts(), loadExternalScripts()).flatten()

    private fun loadInternalScripts(): List<File> {
        val uri = ModuleLoader::class.java.classLoader.getResource("modules").toURI()

        val myPath: Path = (if (uri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(uri, mutableMapOf<String, Any>())
            fileSystem.getPath("modules")
        } else Paths.get(uri))

        val validScripts = mutableListOf<String>()

        Files.walk(myPath, 1).forEach { path ->
            validScripts += path.toString().let { it.removePrefix(it.substringBefore("modules")) }
        }

        return validScripts
            .filter { it.endsWith(".kts") }
            .map { internalResource(it) }
            .toList().filterNotNull()
    }

    private fun loadExternalScripts(): List<File> = classpathResource("modules").listFiles()
        ?.filter { it.extension == "kts" } ?: emptyList()
}
