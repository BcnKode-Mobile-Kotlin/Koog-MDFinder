package com.juzabel.mdfinder.repository

/**
 * Service for checking Ollama endpoint availability via HTTP health check.
 *
 * Performs a GET request to /api/tags endpoint with 2-second timeout.
 * Returns true if Ollama is running and responsive, false otherwise.
 *
 * Implementation is platform-specific (expect/actual pattern).
 */
class OllamaHealthCheck(
    private val endpoint: String = "http://localhost:11434"
) {
    /**
     * Performs an async health check to the Ollama endpoint.
     *
     * - URL: $endpoint/api/tags
     * - Method: HTTP GET
     * - Timeout: 2 seconds (connectTimeout and readTimeout)
     * - Success: HTTP 200 response code
     * - Failure: Any exception or non-200 response code
     *
     * @return true if health check succeeds (HTTP 200), false otherwise
     */
    suspend fun checkHealth(): Boolean = performHealthCheck(endpoint)

    suspend fun checkHealth(customEndpoint: String): Boolean = performHealthCheck(customEndpoint)
}

/**
 * Platform-specific implementation of HTTP health check.
 * Each platform (JVM, Android, iOS) provides its own implementation.
 */
expect suspend fun performHealthCheck(endpoint: String): Boolean
