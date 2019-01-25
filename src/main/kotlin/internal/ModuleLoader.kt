package com.serebit.autotitan.internal

import com.serebit.autotitan.BotConfig
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
import kotlin.streams.toList

internal class ModuleLoader {
    private val compiler = ScriptCompiler()

    suspend fun loadModules(config: BotConfig): List<Module> = coroutineScope {
        loadScripts().map { scriptFile ->
            async {
                Logger.debug("Loading module from file ${scriptFile.name}...")
                compiler.eval(scriptFile)
            }
        }.awaitAll()

        Logger.debug("Finished loading modules.")

        ScriptContext.popModules().map { it.build(config) }
    }

    private fun loadScripts() = loadInternalScripts() + loadExternalScripts()

    private fun loadInternalScripts(): List<File> {
        val modulesUri = ModuleLoader::class.java.classLoader.getResource("modules").toURI()

        val modulesPath: Path = (if (modulesUri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(modulesUri, mutableMapOf<String, Any>())
            fileSystem.getPath("modules")
        } else Paths.get(modulesUri))

        val validScripts = Files.walk(modulesPath, 1).map { path ->
            path.toString().let { it.removePrefix(it.substringBefore("modules")) }
        }.toList()

        return validScripts.filter { it.endsWith(ScriptCompiler.SCRIPT_EXTENSION) }.mapNotNull { internalResource(it) }
    }

    private fun loadExternalScripts(): List<File> = classpathResource("modules").listFiles()
        ?.filter { it.extension == ScriptCompiler.SCRIPT_EXTENSION } ?: emptyList()
}

@PublishedApi
internal object ScriptContext {
    private val moduleTemplates = mutableListOf<ModuleTemplate>()

    @PublishedApi
    internal fun pushModule(template: ModuleTemplate) {
        moduleTemplates += template
    }

    internal fun popModules() = moduleTemplates.toList().also { moduleTemplates.clear() }
}
