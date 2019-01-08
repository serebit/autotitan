package com.serebit.autotitan.network

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.logkat.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.io.jvm.nio.copyTo
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

internal object GithubApi {
    private val serializer = Gson()
    private val client = HttpClient()

    suspend fun getLatestRelease(user: String, repo: String): Release? = if (ping("github.com")) {
        val response = client.call("https://api.github.com/repos/$user/$repo/releases/latest").response
        when (response.status) {
            HttpStatusCode.OK -> serializer.fromJson<Release>(response.readText())
            HttpStatusCode.NotFound -> {
                Logger.warn("Request for latest release from $user/$repo returned nothing.")
                null
            }
            else -> {
                Logger.warn("Request for latest release from $user/$repo returned status ${response.status.value}.")
                null
            }
        }
    } else {
        Logger.warn("Failed to connect to GitHub.")
        null
    }

    suspend inline fun getLatestRelease(user: String, repo: String, filter: (Release) -> Boolean): Release? =
        getLatestRelease(user, repo)?.let { if (filter(it)) it else null }

    data class Release(val name: String, val tag_name: String, val assets: Set<Asset>)

    data class Asset(val content_type: String, private val url: String) {
        suspend fun streamTo(file: File) = FileChannel.open(file.toPath(), StandardOpenOption.WRITE).use {
            client.call(url).response.content.copyTo(it)
        }
    }
}
