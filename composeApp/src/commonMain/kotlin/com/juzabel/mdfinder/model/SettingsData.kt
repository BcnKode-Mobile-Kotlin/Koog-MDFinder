package com.juzabel.mdfinder.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val folderPath: String? = null,
    val selectedModel: String? = null,
    val ollamaEndpoint: String = "http://localhost:11434",
    val geminiApiKey: String = "",
    val isDarkTheme: Boolean = false
) {
    companion object {
        fun defaults(): SettingsData = SettingsData()
    }
}
