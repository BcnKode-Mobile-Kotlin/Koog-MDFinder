package com.juzabel.mdfinder.agent

import com.juzabel.mdfinder.data.MarkdownFile
import com.juzabel.mdfinder.model.ChatMessage

actual suspend fun koogRunAgent(
    query: String,
    files: List<MarkdownFile>,
    conversationHistory: List<ChatMessage>,
    apiKeyOrEndpoint: String,
    modelTag: String,
    isGemini: Boolean
): String {
    throw UnsupportedOperationException("Real agent execution not supported on Android in v1.x")
}
