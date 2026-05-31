package com.juzabel.mdfinder.agent

import com.juzabel.mdfinder.data.MarkdownFile
import com.juzabel.mdfinder.model.ChatMessage

/**
 * Platform bridge for Koog agent execution using the Tool pattern.
 * JVM (desktop): creates a real AIAgent with MarkdownSearchTool via Koog's SimpleTool API.
 * Android/iOS: throws UnsupportedOperationException (deferred to v2.x).
 *
 * @param query User's natural language query (passed to agent.run())
 * @param files Markdown files available for the agent to search
 * @param conversationHistory Previous chat messages for context
 * @param apiKeyOrEndpoint For Gemini: API key. For Ollama: endpoint URL.
 * @param modelTag For Ollama: model id (e.g. "gemma4"). Ignored for Gemini.
 * @param isGemini true = use Google Gemini client, false = use OllamaClient with direct prompt
 * @return Agent response string
 */
expect suspend fun koogRunAgent(
    query: String,
    files: List<MarkdownFile>,
    conversationHistory: List<ChatMessage>,
    apiKeyOrEndpoint: String,
    modelTag: String,
    isGemini: Boolean
): String
