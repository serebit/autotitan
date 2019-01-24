package com.serebit.autotitan.internal

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import kotlinx.coroutines.io.jvm.nio.copyTo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream

enum class RepoOrigin { GITLAB, GITHUB; }

data class ModuleRepo(val modules: List<File>, val version: String)

@Serializable
data class RepoConfig(val modules: List<String>, val version: String)

class RepoLoader {
    private val client = HttpClient()

    fun resolveRemote(origin: RepoOrigin, user: String, repoName: String): RepoConfig? = when (origin) {
        RepoOrigin.GITLAB -> resolveGitlab(user, repoName)
        else -> TODO()
    }

    fun resolveGitlab(user: String, name: String): RepoConfig? {
        val response = runBlocking {
            val encoded = "$user/$name"

            println(encoded)

            client.call("https://gitlab.com/api/v4/projects/$encoded/repository/archive.zip").response
        }
        val tempFile = createTempFile()
        FileChannel.open(tempFile.toPath(), StandardOpenOption.WRITE).use { fc ->
            runBlocking { response.content.copyTo(fc) }
        }
        val tempDir = createTempDir()
        tempFile.unzip(tempDir)

        val configFile = tempDir.resolve("config.json")
        return if (configFile.exists()) {
            Json.unquoted.parse(RepoConfig.serializer(), configFile.readText())
        } else null
    }

    private fun File.unzip(dest: File) = ZipInputStream(FileInputStream(this)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val newFile = File("${dest.path}${File.separator}${entry.name}")
            if (entry.isDirectory) {
                newFile.mkdir()
            } else {
                newFile.createNewFile()
                FileOutputStream(newFile).use {
                    it.write(zis.readBytes())
                }
            }
            entry = zis.nextEntry
        }
    }
}
