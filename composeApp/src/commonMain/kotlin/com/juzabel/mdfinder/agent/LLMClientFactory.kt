package com.juzabel.mdfinder.agent

/**
 * Factory for creating and managing LLM clients for different providers.
 *
 * Handles instantiation of Ollama clients for local models (Qwen3, OLMo3-Think)
 * and Google clients for cloud models (Gemini).
 *
 * In Phase 2, Google client instantiation is scaffolded (API key not yet collected).
 * Full implementation with actual API calls deferred to Phase 3.
 */
object LLMClientFactory {
    /**
     * Represents an LLM client for a specific provider.
     * In Phase 2, this is a type-safe marker; Phase 3 will add actual API methods.
     */
    sealed class LLMClient {
        data class OllamaClient(val endpoint: String = "http://localhost:11434") : LLMClient()
        data class GoogleClient(val apiKey: String? = null) : LLMClient()
    }

    /**
     * Creates an Ollama LLM client configured for the specified endpoint.
     *
     * @param endpoint URL to Ollama service (default: http://localhost:11434)
     * @return OllamaClient instance
     */
    fun createOllamaClient(endpoint: String = "http://localhost:11434"): LLMClient.OllamaClient {
        return LLMClient.OllamaClient(endpoint)
    }

    /**
     * Creates a Google LLM client with the provided API key.
     *
     * @param apiKey Google API key for Gemini (can be null in Phase 2, required in Phase 3)
     * @return GoogleClient instance
     */
    fun createGoogleClient(apiKey: String? = null): LLMClient.GoogleClient {
        return LLMClient.GoogleClient(apiKey)
    }

    /**
     * Creates a multi-executor supporting both Ollama and Google clients.
     *
     * Returns a configured executor that routes queries based on selected model.
     * In Phase 2, this returns a ModelExecutor with clients for both providers.
     * Phase 3 will enhance with actual API calls and authentication.
     *
     * @param ollamaEndpoint URL to Ollama service (default: localhost:11434)
     * @param googleApiKey Google API key (null in Phase 2, collected in Phase 3)
     * @return ModelExecutor configured with both providers
     */
    fun createMultiExecutor(
        ollamaEndpoint: String = "http://localhost:11434",
        googleApiKey: String? = null
    ): ModelExecutor {
        val ollamaClient = createOllamaClient(ollamaEndpoint)
        val googleClient = createGoogleClient(googleApiKey)

        // In Phase 2, both clients are created but only used for routing/structure.
        // Phase 3 will wire actual API calls to Koog or alternative LLM framework.
        return ModelExecutor()
    }
}
