package com.serebit.autotitan.network

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.logkat.Logger
import khttp.get
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection

internal object GithubApi {
    private val serializer = Gson()

    fun getLatestRelease(user: String, repo: String): Release? = if (ping("github.com")) {
        val response = get("https://api.github.com/repos/$user/$repo/releases/latest")
        when (response.statusCode) {
            HttpURLConnection.HTTP_OK -> serializer.fromJson<Release>(response.text)
            HttpURLConnection.HTTP_NOT_FOUND -> {
                Logger.warn("Request for latest release from repo $user/$repo returned nothing.")
                null
            }
            else -> {
                Logger.warn("Request for latest release from repo $user/$repo returned status ${response.statusCode}.")
                null
            }
        }
    } else {
        Logger.warn("Failed to connect to GitHub.")
        null
    }

    inline fun getLatestRelease(user: String, repo: String, filter: (Release) -> Boolean): Release? =
        getLatestRelease(user, repo)?.let { if (filter(it)) it else null }

    data class Release(val name: String, val tag_name: String, val assets: Set<Asset>)

    data class Asset(val content_type: String, private val url: String) {
        fun streamTo(file: File): Long =
            get(url, headers = mapOf("Accept" to "application/octet-stream"), stream = true)
                .raw
                .copyTo(FileOutputStream(file))
    }
}
