package com.juzabel.mdfinder.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android-specific implementation of HTTP health check using Java's HttpURLConnection.
 * This checks if Ollama is running at the specified endpoint.
 */
actual suspend fun performHealthCheck(endpoint: String): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val url = URL("$endpoint/api/tags")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2000  // 2 seconds
            readTimeout = 2000     // 2 seconds
        }

        try {
            val responseCode = conn.responseCode
            responseCode == 200
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        // Any exception (timeout, connection refused, etc.) means Ollama is unavailable
        false
    }
}
