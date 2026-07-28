package com.fitlife.ai.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent> = emptyList(),
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

@Serializable
data class GeminiContent(
    val role: String = "",
    val parts: List<GeminiPart> = emptyList()
)

@Serializable
data class GeminiPart(
    val text: String = ""
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 2048,
    val topP: Double = 0.9,
    val topK: Int = 40
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent = GeminiContent()
)

@Singleton
class GeminiApi @Inject constructor() {
    companion object {
        const val MODEL = "gemini-2.0-flash"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    suspend fun generateContent(
        apiKey: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>
    ): Result<String> {
        return try {
            val contents = mutableListOf<GeminiContent>()
            contents.add(GeminiContent(role = "user", parts = listOf(systemPrompt)))
            contents.add(GeminiContent(role = "model", parts = listOf("Understood. I'm FitLife AI, a fitness coach. I'll help with workouts, nutrition, and health.")))
            for ((role, text) in messages) {
                contents.add(GeminiContent(role = role, parts = listOf(text)))
            }
            val request = GeminiRequest(contents = contents)
            val response = client.post("$BASE_URL/models/$MODEL:generateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val geminiResponse = response.body<GeminiResponse>()
            val text = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I couldn't generate a response. Please try again."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
