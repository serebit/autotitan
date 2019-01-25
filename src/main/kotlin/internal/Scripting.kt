package com.serebit.autotitan.internal

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

@KotlinScript(
    displayName = "Autotitan Module",
    fileExtension = ScriptCompiler.SCRIPT_EXTENSION,
    compilationConfiguration = CompilationConfiguration::class
)
internal abstract class AutotitanModuleScript

internal class CompilationConfiguration : ScriptCompilationConfiguration({
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    defaultImports(
        "com.serebit.autotitan.api.*", "com.serebit.autotitan.extensions.*",
        "net.dv8tion.jda.core.*"
    )
})

internal class ScriptCompiler {
    private val host = BasicJvmScriptingHost()
    private val config = createJvmCompilationConfigurationFromTemplate<AutotitanModuleScript>()

    fun eval(file: File) {
        host.eval(file.readText().toScriptSource(), config, null)
            .reports.forEach { println(it.message) }
    }

    companion object {
        const val SCRIPT_EXTENSION = "module.kts"
    }
}
