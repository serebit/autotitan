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
import kotlin.streams.toList

internal class ModuleLoader {
    suspend fun loadModules(): List<Module> = coroutineScope {
        val engineFactory = ScriptEngineManager().getEngineByExtension("kts").factory!!
        loadScripts().map {
            async {
                Logger.debug("Loading module from file ${it.name}.")
                engineFactory.scriptEngine.eval(it.readText()) as ModuleTemplate
            }
        }.awaitAll().map { it.build() }
    }

    private fun loadScripts() = loadInternalScripts() + loadExternalScripts()

    private fun loadInternalScripts(): List<File> {
        val uri = ModuleLoader::class.java.classLoader.getResource("modules").toURI()

        val myPath: Path = (if (uri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(uri, mutableMapOf<String, Any>())
            fileSystem.getPath("modules")
        } else Paths.get(uri))

        val validScripts = Files.walk(myPath, 1).map { path ->
            path.toString().let { it.removePrefix(it.substringBefore("modules")) }
        }.toList()

        return validScripts
            .filter { it.endsWith(".kts") }
            .map { internalResource(it) }
            .toList().filterNotNull()
    }

    private fun loadExternalScripts(): List<File> = classpathResource("modules").listFiles()
        ?.filter { it.extension == "kts" } ?: emptyList()
}
