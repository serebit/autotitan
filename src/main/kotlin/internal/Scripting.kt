package com.serebit.autotitan.internal

import com.serebit.autotitan.api.logger
import com.serebit.logkat.error
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

@KotlinScript(displayName = "Autotitan Module", fileExtension = ScriptCompiler.SCRIPT_EXTENSION)
internal abstract class AutotitanModuleScript

internal class ScriptCompiler {
    private val host = BasicJvmScriptingHost()
    private val config = createJvmCompilationConfigurationFromTemplate<AutotitanModuleScript> {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
    }

    fun eval(fileName: String, scriptText: String) {
        host.eval(scriptText.toScriptSource(), config, null)
            .reports
            .filter { it.severity == ScriptDiagnostic.Severity.ERROR }
            .forEach { logger.error("Error in ${fileName}: ${it.message}") }
    }

    companion object {
        const val SCRIPT_EXTENSION = "module.kts"
    }
}
