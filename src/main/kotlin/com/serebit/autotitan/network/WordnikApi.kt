package com.serebit.autotitan.network

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import khttp.get
import java.net.HttpURLConnection
import java.net.URI

internal object WordnikApi {
    private val serializer = Gson()
    private var apiKey: String? = null
    private val definitionCache: MutableMap<String, List<Definition>> = mutableMapOf()
    private val relatedWordsCache: MutableMap<String, List<Related>> = mutableMapOf()
    val isInitialized get() = apiKey != null

    fun init(apiKey: String): Boolean {
        val apiTokenStatus = serializer.fromJson<ApiTokenStatus>(
            get(ApiTokenStatus.endpoint, params = mapOf("api_key" to apiKey)).text
        )
        return if (apiTokenStatus.valid) {
            this.apiKey = apiKey
            true
        } else false
    }

    fun getDefinition(word: String, index: Int = 0): Definition? = apiKey?.let {
        if (existsOrCacheDefinition(word)) definitionCache[word]?.getOrNull(index) else null
    }

    fun hasDefinitions(word: String): Boolean = isInitialized && existsOrCacheDefinition(word)

    fun numDefinitions(word: String): Int = apiKey?.let {
        if (existsOrCacheDefinition(word)) definitionCache[word]!!.size else 0
    } ?: -1

    fun getRelatedWords(word: String): List<Related>? = apiKey?.let {
        if (existsOrCacheRelatedWords(word)) relatedWordsCache[word] else null
    }

    fun hasRelatedWords(word: String): Boolean = isInitialized && existsOrCacheRelatedWords(word)

    private fun existsOrCacheDefinition(word: String): Boolean = apiKey?.let { apiKey ->
        if (word !in definitionCache) {
            val response = get(
                Definition.endpointOf(word), params = mapOf(
                    "word" to word,
                    "limit" to "24",
                    "includeRelated" to "false",
                    "useCanonical" to "false",
                    "includeTags" to "false",
                    "api_key" to apiKey
                )
            )

            if (response.statusCode == HttpURLConnection.HTTP_OK) {
                definitionCache[word] = serializer.fromJson(response.text)
                true
            } else false
        } else true
    } ?: false

    private fun existsOrCacheRelatedWords(word: String): Boolean = apiKey?.let { apiKey ->
        if (word !in relatedWordsCache) {
            val response = get(
                Related.endpointOf(word), params = mapOf(
                    "word" to word,
                    "useCanonical" to "true",
                    "relationshipTypes" to "synonym,antonym",
                    "limitPerRelationshipType" to "12",
                    "api_key" to apiKey
                )
            )

            if (response.statusCode == HttpURLConnection.HTTP_OK) {
                relatedWordsCache[word] = serializer.fromJson(response.text)
                true
            } else false
        } else true
    } ?: false

    data class Definition(val partOfSpeech: String, val text: String) {
        companion object {
            fun endpointOf(query: String): String =
                URI("http", "api.wordnik.com", "/v4/word.json/$query/definitions", null).toASCIIString()
        }
    }

    data class Related(val relationshipType: String, val words: List<String>) {
        companion object {
            fun endpointOf(word: String): String =
                URI("http", "api.wordnik.com", "/v4/word.json/$word/relatedWords", null).toASCIIString()
        }
    }

    data class ApiTokenStatus(val valid: Boolean) {
        companion object {
            const val endpoint = "http://api.wordnik.com/v4/account.json/apiTokenStatus"
        }
    }
}
