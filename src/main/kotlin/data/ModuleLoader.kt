package com.serebit.autotitan.data

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.logkat.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.ScriptEngineManager

internal class ModuleLoader {
    suspend fun loadModules(): List<Module> = coroutineScope {
        val scriptEngine = ScriptEngineManager().getEngineByExtension("kts").factory!!.scriptEngine
        loadScripts().map {
            async {
                Logger.debug("Loading module from file ${it.name}.")
                scriptEngine.eval(it.readText()) as ModuleTemplate
            }
        }.awaitAll().map { it.build() }
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
