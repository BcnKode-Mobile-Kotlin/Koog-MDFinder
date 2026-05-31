package com.juzabel.mdfinder.repository

import com.juzabel.mdfinder.agent.ModelExecutor
import com.juzabel.mdfinder.data.MarkdownFile
import com.juzabel.mdfinder.model.AIModel
import com.juzabel.mdfinder.model.ChatMessage
import com.juzabel.mdfinder.model.SettingsData

class AgentRepository {
    data class AgentResponse(
        val summary: String,
        val isError: Boolean = false,
        val errorMessage: String? = null
    )

    private val modelExecutor = ModelExecutor()

    suspend fun queryAgent(
        query: String,
        files: List<MarkdownFile>,
        conversationHistory: List<ChatMessage> = emptyList(),
        model: AIModel = AIModel.GEMINI,
        settingsData: SettingsData = SettingsData()
    ): AgentResponse = try {
        val result = modelExecutor.execute(
            query = query,
            files = files,
            model = model,
            conversationHistory = conversationHistory,
            settingsData = settingsData
        )
        AgentResponse(summary = result)
    } catch (e: Exception) {
        AgentResponse(
            summary = "",
            isError = true,
            errorMessage = "Agent failed: ${e.message}"
        )
    }
}
