package com.juzabel.mdfinder.agent

import com.juzabel.mdfinder.data.MarkdownFile
import com.juzabel.mdfinder.model.AIModel
import com.juzabel.mdfinder.model.ChatMessage
import com.juzabel.mdfinder.model.SettingsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Routes agent execution to the appropriate backend based on selected AI model.
 *
 * Delegates to koogRunAgent (expect/actual) which creates a real AIAgent
 * with MarkdownSearchTool via Koog's SimpleTool + ToolRegistry pattern on JVM.
 */
class ModelExecutor {
    suspend fun execute(
        query: String,
        files: List<MarkdownFile>,
        model: AIModel,
        conversationHistory: List<ChatMessage> = emptyList(),
        settingsData: SettingsData = SettingsData()
    ): String = withContext(Dispatchers.Default) {
        when (model) {
            AIModel.GEMMA4 -> koogRunAgent(
                query = query,
                files = files,
                conversationHistory = conversationHistory,
                apiKeyOrEndpoint = settingsData.ollamaEndpoint,
                modelTag = "gemma4:e2b",
                isGemini = false
            )
            AIModel.GEMINI -> {
                if (settingsData.geminiApiKey.isEmpty()) {
                    throw Exception("Gemini API error: Gemini API key not configured. Add key in Settings.")
                }
                koogRunAgent(
                    query = query,
                    files = files,
                    conversationHistory = conversationHistory,
                    apiKeyOrEndpoint = settingsData.geminiApiKey,
                    modelTag = "",
                    isGemini = true
                )
            }
        }
    }
}
