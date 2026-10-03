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

class FirestoreSyncService(
    private val authService: FirebaseAuthService = FirebaseAuthService()
) {
    companion object {
        const val PROJECT_ID = "dashboard-39cf8"
        const val BASE_FIRESTORE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"
    }

    suspend fun getCollectionItems(
        collectionName: String,
        uid: String,
        idToken: String,
        refreshToken: String = "",
        onTokenRefreshed: (suspend (newToken: String, newRefresh: String) -> Unit)? = null
    ): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        val urlStr = "$BASE_FIRESTORE_URL/users/$uid/data/$collectionName"
        var activeToken = idToken

        var response = sendGet(urlStr, activeToken)

        // Auto refresh token if 401 Unauthorized
        if (response.code == 401 && refreshToken.isNotBlank()) {
            val refreshRes = authService.refreshIdToken(refreshToken)
            if (refreshRes.success && refreshRes.idToken.isNotBlank()) {
                activeToken = refreshRes.idToken
                onTokenRefreshed?.invoke(refreshRes.idToken, refreshRes.refreshToken)
                response = sendGet(urlStr, activeToken)
            }
        }

        if (response.code in 200..299) {
            try {
                val docJson = JSONObject(response.body)
                val fields = docJson.optJSONObject("fields")
                val itemsObj = fields?.optJSONObject("items")
                val arrayVal = itemsObj?.optJSONObject("arrayValue")
                val values = arrayVal?.optJSONArray("values")

                val resultList = mutableListOf<JSONObject>()
                if (values != null) {
                    for (i in 0 until values.length()) {
                        val valObj = values.optJSONObject(i)
                        val mapVal = valObj?.optJSONObject("mapValue")
                        if (mapVal != null) {
                            val mapFields = mapVal.optJSONObject("fields")
                            if (mapFields != null) {
                                val cleanObj = firestoreFieldsToJson(mapFields)
                                resultList.add(cleanObj)
                            }
                        }
                    }
                }
                Result.success(resultList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else if (response.code == 404) {
            // Document does not exist yet on cloud, return empty list
            Result.success(emptyList())
        } else {
            Result.failure(Exception("Firestore GET $collectionName failed with code ${response.code}: ${response.body}"))
        }
    }

    suspend fun setCollectionItems(
        collectionName: String,
        uid: String,
        items: List<JSONObject>,
        idToken: String,
        refreshToken: String = "",
        onTokenRefreshed: (suspend (newToken: String, newRefresh: String) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val urlStr = "$BASE_FIRESTORE_URL/users/$uid/data/$collectionName"
        var activeToken = idToken

        val payload = buildFirestorePayload(items)
        var response = sendPatch(urlStr, payload.toString(), activeToken)

        // Auto refresh token if 401 Unauthorized
        if (response.code == 401 && refreshToken.isNotBlank()) {
            val refreshRes = authService.refreshIdToken(refreshToken)
            if (refreshRes.success && refreshRes.idToken.isNotBlank()) {
                activeToken = refreshRes.idToken
                onTokenRefreshed?.invoke(refreshRes.idToken, refreshRes.refreshToken)
                response = sendPatch(urlStr, payload.toString(), activeToken)
            }
        }

        if (response.code in 200..299) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Firestore PATCH $collectionName failed with code ${response.code}: ${response.body}"))
        }
    }

    // ─── Converters between plain JSONObject and Firestore REST Value format ────
    private fun buildFirestorePayload(items: List<JSONObject>): JSONObject {
        val valuesArray = JSONArray()
        items.forEach { json ->
            val fieldsObj = jsonToFirestoreFields(json)
            valuesArray.put(JSONObject().apply {
                put("mapValue", JSONObject().apply {
                    put("fields", fieldsObj)
                })
            })
        }

        return JSONObject().apply {
            put("fields", JSONObject().apply {
                put("items", JSONObject().apply {
                    put("arrayValue", JSONObject().apply {
                        put("values", valuesArray)
                    })
                })
            })
        }
    }

    private fun jsonToFirestoreFields(json: JSONObject): JSONObject {
        val fields = JSONObject()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.opt(key)
            fields.put(key, anyToFirestoreValue(value))
        }
        return fields
    }

    private fun anyToFirestoreValue(value: Any?): JSONObject {
        val out = JSONObject()
        when (value) {
            null, JSONObject.NULL -> out.put("nullValue", JSONObject.NULL)
            is Boolean -> out.put("booleanValue", value)
            is Long -> out.put("integerValue", value.toString())
            is Int -> out.put("integerValue", value.toString())
            is Double -> out.put("doubleValue", value)
            is Float -> out.put("doubleValue", value.toDouble())
            is String -> out.put("stringValue", value)
            is JSONArray -> {
                val arr = JSONArray()
                for (i in 0 until value.length()) {
                    arr.put(anyToFirestoreValue(value.opt(i)))
                }
                out.put("arrayValue", JSONObject().apply { put("values", arr) })
            }
            is JSONObject -> {
                out.put("mapValue", JSONObject().apply { put("fields", jsonToFirestoreFields(value)) })
            }
            else -> out.put("stringValue", value.toString())
        }
        return out
    }

    private fun firestoreFieldsToJson(fields: JSONObject): JSONObject {
        val out = JSONObject()
        val keys = fields.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val valObj = fields.optJSONObject(key)
            if (valObj != null) {
                out.put(key, firestoreValueToAny(valObj))
            }
        }
        return out
    }

    private fun firestoreValueToAny(valObj: JSONObject): Any {
        if (valObj.has("stringValue")) return valObj.getString("stringValue")
        if (valObj.has("booleanValue")) return valObj.getBoolean("booleanValue")
        if (valObj.has("integerValue")) {
            val s = valObj.getString("integerValue")
            return s.toLongOrNull() ?: 0L
        }
        if (valObj.has("doubleValue")) return valObj.getDouble("doubleValue")
        if (valObj.has("arrayValue")) {
            val arr = valObj.optJSONObject("arrayValue")?.optJSONArray("values")
            val list = JSONArray()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val child = arr.optJSONObject(i)
                    if (child != null) list.put(firestoreValueToAny(child))
                }
            }
            return list
        }
        if (valObj.has("mapValue")) {
            val childFields = valObj.optJSONObject("mapValue")?.optJSONObject("fields")
            return if (childFields != null) firestoreFieldsToJson(childFields) else JSONObject()
        }
        return JSONObject.NULL
    }

    // ─── Network helpers ────────────────────────────────────────────────────────
    private fun sendGet(urlString: String, token: String): NetworkResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 12000
        conn.readTimeout = 12000

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(stream))
        val body = reader.readText()
        reader.close()
        return NetworkResponse(code, body)
    }

    private fun sendPatch(urlString: String, jsonBody: String, token: String): NetworkResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "PATCH"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 12000
        conn.readTimeout = 12000

        OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(stream))
        val body = reader.readText()
        reader.close()
        return NetworkResponse(code, body)
    }

    private data class NetworkResponse(val code: Int, val body: String)
}
