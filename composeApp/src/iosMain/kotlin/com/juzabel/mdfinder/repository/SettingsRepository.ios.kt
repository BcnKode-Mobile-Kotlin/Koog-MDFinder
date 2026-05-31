package com.juzabel.mdfinder.repository

import com.juzabel.mdfinder.model.SettingsData

// TODO: Phase 4+ - Implement iOS-specific settings persistence
actual class SettingsRepository {
    actual suspend fun loadSettings(): SettingsData {
        // Stub for v2 - return defaults
        return SettingsData()
    }

    actual suspend fun saveSettings(settings: SettingsData): Boolean {
        // Stub for v2 - return success
        return true
    }
}
