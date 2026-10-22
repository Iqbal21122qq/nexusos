package com.nexusos.agent.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.nexusos.agent.data.local.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiAgentClient(private val apiKeyManager: ApiKeyManager) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Free-Tier Rate-Limit Queue Manager
    private val rateLimitMutex = Mutex()
    private var lastRequestTimeMs = 0L

    private suspend fun enforceFreeTierRateLimit() {
        rateLimitMutex.withLock {
            val rpm = apiKeyManager.getRpmLimit()
            val minIntervalMs = (60_000L / rpm) + 250L // Safe buffer
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTimeMs
            if (elapsed < minIntervalMs) {
                delay(minIntervalMs - elapsed)
            }
            lastRequestTimeMs = System.currentTimeMillis()
        }
    }

    /**
     * Compresses screenshot to WebP (Quality 60) to minimize token usage on Free Tier
     */
    fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, 720, 1280, true)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.WEBP, 60, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeScreenAndDecide(
        userGoal: String,
        uiXmlHierarchy: String,
        screenshotBase64: String?,
        ragContext: String = "",
        useLiteModel: Boolean = false
    ): Result<AgentDecision> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY সেট করা নেই! Settings থেকে API Key দিন।"))
        }

        enforceFreeTierRateLimit()

        val modelName = if (useLiteModel) {
            apiKeyManager.getLiteModel()
        } else {
            apiKeyManager.getPrimaryModel()
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val systemInstruction = """
            You are NexusOS Agent, an autonomous Android assistant.
            Analyze the screen UI XML tree and screenshot to fulfill the user's goal.
            RULES:
            1. Respond strictly in valid JSON matching the schema:
               {"action": "TAP|SWIPE|TYPE|SHELL|ASK_USER|COMPLETE", "coordinates": [x, y], "text": "...", "reasoning": "...", "requiresConfirmation": true/false, "spokenResponseBn": "..."}
            2. Set "requiresConfirmation": true BEFORE sending any message, placing any freelance bid, publishing a post, or opening financial apps.
            3. Never run destructive shell commands.
        """.trimIndent()

        val partsArray = buildJsonArray {
            add(buildJsonObject {
                put("text", "USER GOAL: $userGoal\nRAG MEMORY: $ragContext\nUI TREE:\n${uiXmlHierarchy.take(12000)}")
            })
            if (!screenshotBase64.isNullOrBlank()) {
                add(buildJsonObject {
                    put("inlineData", buildJsonObject {
                        put("mimeType", "image/webp")
                        put("data", screenshotBase64)
                    })
                })
            }
        }

        val requestPayload = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", partsArray)
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.failure(RuntimeException("Gemini API Error (${response.code}): $bodyStr"))
                }

                val rootJson = json.parseToJsonElement(bodyStr).jsonObject
                val rawText = rootJson["candidates"]?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("content")?.jsonObject?.get("parts")
                    ?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.content
                    ?: throw IllegalStateException("Empty response from Gemini")

                val decision = json.decodeFromString<AgentDecision>(rawText)
                Result.success(decision)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
