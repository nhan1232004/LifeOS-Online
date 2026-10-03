package com.nhan.lifeos.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class AuthResult(
    val success: Boolean,
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val idToken: String = "",
    val refreshToken: String = "",
    val errorMessage: String? = null
)

data class RefreshResult(
    val success: Boolean,
    val idToken: String = "",
    val refreshToken: String = "",
    val errorMessage: String? = null
)

class FirebaseAuthService {

    companion object {
        const val FIREBASE_API_KEY = "AIzaSyDOv1gI7x-9ljsrabTCH6qnsmXtteEtMO0"
        const val AUTH_SIGNIN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$FIREBASE_API_KEY"
        const val AUTH_SIGNUP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$FIREBASE_API_KEY"
        const val TOKEN_REFRESH_URL = "https://securetoken.googleapis.com/v1/token?key=$FIREBASE_API_KEY"
    }

    suspend fun signIn(email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
                put("returnSecureToken", true)
            }

            val response = postJson(AUTH_SIGNIN_URL, payload.toString())
            if (response.code in 200..299) {
                val json = JSONObject(response.body)
                val uid = json.optString("localId")
                val token = json.optString("idToken")
                val refresh = json.optString("refreshToken")
                val mail = json.optString("email", email)
                val name = json.optString("displayName", email.substringBefore("@"))
                AuthResult(
                    success = true,
                    userId = uid,
                    email = mail,
                    displayName = name,
                    idToken = token,
                    refreshToken = refresh
                )
            } else {
                val errMsg = parseFirebaseError(response.body)
                AuthResult(success = false, errorMessage = errMsg)
            }
        } catch (e: Exception) {
            AuthResult(success = false, errorMessage = "Không thể kết nối đến máy chủ: ${e.localizedMessage}")
        }
    }

    suspend fun signUp(email: String, pass: String, displayName: String = ""): AuthResult = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
                put("returnSecureToken", true)
            }

            val response = postJson(AUTH_SIGNUP_URL, payload.toString())
            if (response.code in 200..299) {
                val json = JSONObject(response.body)
                val uid = json.optString("localId")
                val token = json.optString("idToken")
                val refresh = json.optString("refreshToken")
                val mail = json.optString("email", email)
                val name = displayName.ifBlank { email.substringBefore("@") }
                AuthResult(
                    success = true,
                    userId = uid,
                    email = mail,
                    displayName = name,
                    idToken = token,
                    refreshToken = refresh
                )
            } else {
                val errMsg = parseFirebaseError(response.body)
                AuthResult(success = false, errorMessage = errMsg)
            }
        } catch (e: Exception) {
            AuthResult(success = false, errorMessage = "Lỗi đăng ký tài khoản: ${e.localizedMessage}")
        }
    }

    suspend fun refreshIdToken(refreshToken: String): RefreshResult = withContext(Dispatchers.IO) {
        try {
            val postData = "grant_type=refresh_token&refresh_token=$refreshToken"
            val url = URL(TOKEN_REFRESH_URL)
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            OutputStreamWriter(conn.outputStream).use { it.write(postData) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(stream))
            val body = reader.readText()
            reader.close()

            if (code in 200..299) {
                val json = JSONObject(body)
                RefreshResult(
                    success = true,
                    idToken = json.getString("id_token"),
                    refreshToken = json.optString("refresh_token", refreshToken)
                )
            } else {
                RefreshResult(success = false, errorMessage = "Token refresh failed: $code")
            }
        } catch (e: Exception) {
            RefreshResult(success = false, errorMessage = e.localizedMessage)
        }
    }

    private fun postJson(urlString: String, jsonBody: String): NetworkResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(stream))
        val body = reader.readText()
        reader.close()

        return NetworkResponse(code, body)
    }

    private fun parseFirebaseError(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            val rawMsg = errObj?.optString("message") ?: "Lỗi xác thực"
            when {
                rawMsg.contains("EMAIL_NOT_FOUND") -> "Email này chưa được đăng ký tài khoản."
                rawMsg.contains("INVALID_PASSWORD") -> "Mật khẩu không chính xác."
                rawMsg.contains("INVALID_LOGIN_CREDENTIALS") -> "Email hoặc mật khẩu không chính xác."
                rawMsg.contains("EMAIL_EXISTS") -> "Email này đã được sử dụng. Vui lòng đăng nhập."
                rawMsg.contains("USER_DISABLED") -> "Tài khoản này đã bị khóa."
                rawMsg.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Quá nhiều lần thử sai. Vui lòng thử lại sau ít phút."
                rawMsg.contains("WEAK_PASSWORD") -> "Mật khẩu quá yếu (tối thiểu 6 ký tự)."
                else -> rawMsg
            }
        } catch (e: Exception) {
            "Đăng nhập thất bại: $responseBody"
        }
    }

    private data class NetworkResponse(val code: Int, val body: String)
}
