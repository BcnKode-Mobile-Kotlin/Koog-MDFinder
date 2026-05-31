package com.juzabel.mdfinder.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

actual class SettingsValidator actual constructor() {
    actual suspend fun validateOllamaEndpoint(endpoint: String): ValidationResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val url = URL(endpoint + "/api/tags")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.requestMethod = "GET"
                try {
                    val responseCode = conn.responseCode
                    if (responseCode == 200 || responseCode == 304) {
                        ValidationResult.Success
                    } else {
                        ValidationResult.Error("Ollama endpoint returned error code: $responseCode")
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: SocketTimeoutException) {
                ValidationResult.Error("Ollama not reachable at $endpoint. Check the URL and ensure Ollama is running.")
            } catch (e: Exception) {
                ValidationResult.Error("Failed to connect to Ollama: ${e.message}")
            }
        }

    actual suspend fun validateGeminiApiKey(apiKey: String): ValidationResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.requestMethod = "GET"
                try {
                    val responseCode = conn.responseCode
                    when (responseCode) {
                        200 -> ValidationResult.Success
                        401, 403 -> ValidationResult.Error("Invalid Gemini API key. Check your key and try again. Get a key at https://aistudio.google.com/apikey")
                        else -> ValidationResult.Error("Invalid Gemini API key. Get a key at https://aistudio.google.com/apikey")
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                ValidationResult.Error("Invalid Gemini API key. Get a key at https://aistudio.google.com/apikey")
            }
        }
}

actual fun createSettingsValidator(): SettingsValidator = SettingsValidator()
