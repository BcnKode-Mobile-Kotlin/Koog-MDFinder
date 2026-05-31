package com.juzabel.mdfinder.repository

actual class SettingsValidator actual constructor() {
    actual suspend fun validateOllamaEndpoint(endpoint: String): ValidationResult {
        // iOS implementation - stub for now
        return ValidationResult.Success
    }

    actual suspend fun validateGeminiApiKey(apiKey: String): ValidationResult {
        // iOS implementation - stub for now
        return ValidationResult.Success
    }
}

actual fun createSettingsValidator(): SettingsValidator = SettingsValidator()
