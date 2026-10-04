package com.nhan.lifeos.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object GeminiApiService {

    const val DEFAULT_MODEL = "gemini-flash-latest"

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    val ACTIVE_MODELS = listOf(
        "gemini-flash-latest",
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-pro-latest"
    )

    fun cleanApiKey(raw: String): String {
        return raw.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .removePrefix("Bearer ")
            .trim()
    }

    fun isOAuthClientId(key: String): Boolean {
        val lower = key.lowercase()
        return lower.contains(".apps.googleusercontent.com") || lower.startsWith("gocspx-")
    }

    suspend fun testConnection(apiKey: String, model: String = DEFAULT_MODEL): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = cleanApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Vui lòng nhập API Key"))
        }
        if (isOAuthClientId(cleanKey)) {
            return@withContext Result.failure(
                IllegalArgumentException("Mã bạn nhập là Google OAuth Client ID, không phải Gemini API Key! Hãy lấy API Key (AIzaSy...) tại aistudio.google.com/app/apikey")
            )
        }

        val testModel = if (model.contains("2.0") || model.contains("1.5")) DEFAULT_MODEL else model

        try {
            val endpoint = "$BASE_URL/$testModel:generateContent?key=$cleanKey"
            val payload = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Ping test. Trả lời đúng 1 chữ: OK")
                            })
                        })
                    })
                }
                put("contents", contentsArr)
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 10)
                })
            }

            val resp = postJson(endpoint, payload.toString(), cleanKey)
            if (resp.code in 200..299) {
                Result.success("Kết nối Gemini API thành công! Mô hình $testModel đã sẵn sàng phản hồi.")
            } else {
                val errObj = try { JSONObject(resp.body).optJSONObject("error") } catch (_: Exception) { null }
                val errMsg = errObj?.optString("message") ?: "HTTP ${resp.code}: ${resp.body.take(150)}"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateContent(
        apiKey: String,
        preferredModel: String = DEFAULT_MODEL,
        systemInstruction: String,
        history: List<Pair<Boolean, String>>, // (isUser, text)
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = cleanApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Chưa cài đặt Gemini API Key"))
        }

        val validPreferred = if (preferredModel.contains("2.0") || preferredModel.contains("1.5")) DEFAULT_MODEL else preferredModel
        val modelsToTry = (listOf(validPreferred) + ACTIVE_MODELS).distinct()

        var lastError: Exception = Exception("Không thể kết nối đến Gemini API")

        for (model in modelsToTry) {
            try {
                val endpoint = "$BASE_URL/$model:generateContent?key=$cleanKey"

                val payload = JSONObject().apply {
                    // System Instruction
                    if (systemInstruction.isNotBlank()) {
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", systemInstruction) })
                            })
                        })
                    }

                    // Multi-turn history (last 12 turns max to prevent exceeding token limit)
                    val contentsArr = JSONArray()
                    val recentHistory = history.takeLast(12)
                    for ((isUser, text) in recentHistory) {
                        contentsArr.put(JSONObject().apply {
                            put("role", if (isUser) "user" else "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", text) })
                            })
                        })
                    }

                    // Current new user query
                    contentsArr.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userMessage) })
                        })
                    })

                    put("contents", contentsArr)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 2048)
                        put("topP", 0.95)
                    })
                }

                val resp = postJson(endpoint, payload.toString(), cleanKey)
                if (resp.code in 200..299) {
                    val root = JSONObject(resp.body)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val replyText = parts.getJSONObject(0).optString("text")
                            if (replyText.isNotBlank()) {
                                return@withContext Result.success(replyText.trim())
                            }
                        }
                    }
                    return@withContext Result.failure(Exception("Gemini không trả về nội dung hợp lệ."))
                } else {
                    val errObj = try { JSONObject(resp.body).optJSONObject("error") } catch (_: Exception) { null }
                    val errMsg = errObj?.optString("message") ?: "HTTP ${resp.code}: ${resp.body.take(150)}"

                    val lower = errMsg.lowercase()
                    if (lower.contains("api key not valid") || lower.contains("invalid api key") || lower.contains("api_key_invalid")) {
                        return@withContext Result.failure(Exception("Gemini API Key không hợp lệ. Vui lòng kiểm tra lại."))
                    }
                    if (lower.contains("oauth") || lower.contains("authentication credentials")) {
                        return@withContext Result.failure(Exception("Key bạn nhập là OAuth Client ID, không phải Gemini API Key!"))
                    }

                    lastError = Exception(errMsg)
                    // Try fallback model
                    continue
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError)
    }

    private data class HttpResponse(val code: Int, val body: String)

    private fun postJson(urlString: String, jsonBody: String, apiKey: String): HttpResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", apiKey)
        conn.connectTimeout = 25000
        conn.readTimeout = 30000
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(stream ?: conn.inputStream, "UTF-8"))
        val response = reader.readText()
        reader.close()
        conn.disconnect()

        return HttpResponse(code, response)
    }
}
