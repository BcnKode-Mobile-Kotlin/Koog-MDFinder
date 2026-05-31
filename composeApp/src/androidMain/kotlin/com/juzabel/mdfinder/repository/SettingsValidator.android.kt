package com.juzabel.mdfinder.repository

// TODO: Phase 4+ - Implement Android-specific validation
actual class SettingsValidator actual constructor() {
    actual suspend fun validateOllamaEndpoint(endpoint: String): ValidationResult {
        // Stub for Phase 3 (desktop-only phase)
        return ValidationResult.Success
    }

    actual suspend fun validateGeminiApiKey(apiKey: String): ValidationResult {
        // Stub for Phase 3 (desktop-only phase)
        return ValidationResult.Success
    }
}

actual fun createSettingsValidator(): SettingsValidator = SettingsValidator()
