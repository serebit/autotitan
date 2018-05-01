package com.serebit.autotitan.apiwrappers

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.loggerkt.Logger
import khttp.get
import java.io.File
import java.io.FileOutputStream

internal object GithubApi {
    private val serializer = Gson()

    fun getLatestRelease(user: String, repo: String): Release? {
        val response = get("https://api.github.com/repos/$user/$repo/releases/latest")
        return when (response.statusCode) {
            200 -> serializer.fromJson(response.text)
            404 -> {
                Logger.warn("Request for latest release from repo $user/$repo returned nothing.")
                null
            }
            else -> {
                Logger.warn("Request for latest release from repo $user/$repo returned status ${response.statusCode}.")
                null
            }
        }
    }

    data class Release(val name: String, val tag_name: String, val assets: Set<Asset>)

    data class Asset(val content_type: String, private val url: String) {
        fun streamTo(file: File): Long =
            get(url, headers = mapOf("Accept" to "application/octet-stream"), stream = true)
                .raw
                .copyTo(FileOutputStream(file))
    }
}
