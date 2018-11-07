package com.serebit.autotitan.network

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import khttp.get
import java.net.HttpURLConnection
import java.net.URLEncoder

internal object UrbanDictionaryApi {
    private val serializer = Gson()
    private val resultCache: MutableMap<String, List<Definition>> = mutableMapOf()

    fun hasDefinitions(query: String) = existsOrCacheDefinition(query) && resultCache[query]!!.isNotEmpty()

    fun numDefinitions(query: String) = if (existsOrCacheDefinition(query)) resultCache[query]!!.size else 0

    fun getDefinition(query: String, index: Int): Definition? = if (existsOrCacheDefinition(query)) {
        resultCache[query]?.getOrNull(index)
    } else null

    private fun existsOrCacheDefinition(query: String): Boolean = if (query in resultCache) true else {
        val response = get("https://api.urbandictionary.com/v0/define?term=${URLEncoder.encode(query, "UTF-8")}")
        when {
            response.statusCode == HttpURLConnection.HTTP_OK -> {
                resultCache[query] = serializer.fromJson<Result>(response.text).list
                true
            }
            response.jsonObject["result_type"] != "no_results" -> {
                resultCache[query] = emptyList()
                false
            }
            else -> false
        }
    }

    private data class Result(val list: List<Definition>)

    data class Definition(val definition: String, val permalink: String, val example: String)
}
