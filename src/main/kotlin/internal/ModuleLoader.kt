package com.serebit.autotitan.internal

import com.serebit.autotitan.BotConfig
import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.logger
import com.serebit.logkat.debug
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.streams.asSequence

internal class ModuleLoader {
    private val compiler = ScriptCompiler()

    suspend fun loadModules(config: BotConfig): List<Module> = coroutineScope {
        loadScripts().map { (fileName, scriptFile) ->
            async {
                logger.debug("Loading module from file $fileName...")
                compiler.eval(scriptFile)
            }
        }.awaitAll()

        logger.debug("Finished loading modules.")

        ScriptContext.popModules().map { it.build(config) }
    }

    private fun loadScripts() = loadInternalScripts() + loadExternalScripts()

    private fun loadInternalScripts(): List<Pair<String, File>> {
        val modulesUri = ModuleLoader::class.java.classLoader.getResource("modules")!!.toURI()
        val modulesPath = FileSystems.newFileSystem(modulesUri, mutableMapOf<String, Any>()).getPath("modules")

        return Files.walk(modulesPath, 1).asSequence()
            .map { it.toString() }
            .map { it.removePrefix(it.substringBefore("modules")) }
            .filter { it.endsWith(ScriptCompiler.SCRIPT_EXTENSION) }
            .mapNotNull { path -> internalResource(path)?.let { path to it } }
            .toList()
    }

    private fun loadExternalScripts(): List<Pair<String, File>> = classpathResource("modules").listFiles()
        ?.filter { it.extension == ScriptCompiler.SCRIPT_EXTENSION }
        ?.map { it.name to it }
        ?: emptyList()
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
