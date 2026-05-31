package com.juzabel.mdfinder.repository

import com.juzabel.mdfinder.model.SettingsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import java.nio.file.Files

actual class SettingsRepository {
    actual suspend fun loadSettings(): SettingsData =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val file = getConfigFile()
                if (!file.exists()) return@withContext SettingsData()
                val content = Files.readString(file.toPath())
                Json.decodeFromString<SettingsData>(content)
            } catch (e: SerializationException) {
                logError("Settings file corrupted: ${e.message}")
                SettingsData()
            } catch (e: IOException) {
                logError("Failed to read settings: ${e.message}")
                SettingsData()
            }
        }

    actual suspend fun saveSettings(settings: SettingsData): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val file = getConfigFile()
                file.parentFile?.mkdirs()  // Ensure ~/.config/mdfinder/ exists
                val json = Json.encodeToString(settings)
                Files.writeString(file.toPath(), json)
                true
            } catch (e: Exception) {
                logError("Failed to save settings: ${e.message}")
                false
            }
        }

    private fun getConfigFile(): File {
        val userHome = System.getProperty("user.home")
        val configDir = File(userHome, ".config${File.separator}mdfinder")
        return File(configDir, "settings.json")
    }

    private fun logError(message: String) {
        System.err.println("[SettingsRepository] $message")
    }
}
