package com.juzabel.mdfinder.repository

import com.juzabel.mdfinder.model.SettingsData

expect class SettingsRepository() {
    suspend fun loadSettings(): SettingsData
    suspend fun saveSettings(settings: SettingsData): Boolean
}
