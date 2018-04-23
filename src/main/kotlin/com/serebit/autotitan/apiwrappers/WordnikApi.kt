package com.serebit.autotitan.apiwrappers

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import khttp.get
import java.net.HttpURLConnection

internal object WordnikApi {
    private val serializer = Gson()
    private lateinit var apiKey: String
    private val definitionCache: MutableMap<String, List<Definition>> = mutableMapOf()
    private val relatedWordsCache: MutableMap<String, List<Related>> = mutableMapOf()
    val isInitialized get() = ::apiKey.isInitialized

    fun init(apiKey: String): Boolean {
        val apiTokenStatus = serializer.fromJson<ApiTokenStatus>(
            get(ApiTokenStatus.endpoint, params = mapOf("api_key" to apiKey)).text
        )
        if (apiTokenStatus.valid) {
            this.apiKey = apiKey
        }
        return apiTokenStatus.valid
    }

    fun getDefinition(word: String, index: Int = 0): Definition? = when {
        !isInitialized -> null
        existsOrCacheDefinition(word) -> definitionCache[word]?.getOrNull(index)
        else -> null
    }

    fun hasDefinitions(word: String): Boolean = if (isInitialized) existsOrCacheDefinition(word) else false

    fun numDefinitions(word: String): Int = when {
        !isInitialized -> -1
        existsOrCacheDefinition(word) -> definitionCache[word]?.size ?: 0
        else -> 0
    }

    fun getRelatedWords(word: String): List<Related>? = when {
        !isInitialized -> null
        existsOrCacheRelatedWords(word) -> relatedWordsCache[word]
        else -> null
    }

    fun hasRelatedWords(word: String): Boolean = if (isInitialized) existsOrCacheRelatedWords(word) else false

    private fun existsOrCacheDefinition(word: String): Boolean = when {
        !isInitialized -> false
        word in definitionCache -> true
        else -> {
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
        }
    }

    private fun existsOrCacheRelatedWords(word: String): Boolean = when {
        !isInitialized -> false
        word in relatedWordsCache -> true
        else -> {
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
        }
    }

    data class Definition(val partOfSpeech: String, val text: String) {
        companion object {
            fun endpointOf(word: String) = "http://api.wordnik.com/v4/word.json/$word/definitions"
        }
    }

    data class Related(val relationshipType: String, val words: List<String>) {
        companion object {
            fun endpointOf(word: String) = "http://api.wordnik.com/v4/word.json/$word/relatedWords"
        }
    }

    data class ApiTokenStatus(val valid: Boolean) {
        companion object {
            const val endpoint = "http://api.wordnik.com/v4/account.json/apiTokenStatus"
        }
    }
}
