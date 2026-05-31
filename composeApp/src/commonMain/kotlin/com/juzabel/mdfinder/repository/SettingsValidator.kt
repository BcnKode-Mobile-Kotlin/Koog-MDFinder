package com.juzabel.mdfinder.repository

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

expect class SettingsValidator() {
    suspend fun validateOllamaEndpoint(endpoint: String): ValidationResult
    suspend fun validateGeminiApiKey(apiKey: String): ValidationResult
}

expect fun createSettingsValidator(): SettingsValidator
