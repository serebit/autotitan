package com.serebit.autotitan.internal

import java.io.File
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

    fun eval(file: File) {
        host.eval(file.readText().toScriptSource(), config, null)
            .reports
            .filter { it.severity == ScriptDiagnostic.Severity.ERROR }.forEach { println(it.message) }
    }

    companion object {
        const val SCRIPT_EXTENSION = "module.kts"
    }
}
