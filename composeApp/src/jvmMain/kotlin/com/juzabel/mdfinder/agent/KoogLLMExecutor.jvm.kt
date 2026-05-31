package com.juzabel.mdfinder.agent

import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.rag.base.files.JVMFileSystemProvider
import com.juzabel.mdfinder.data.MarkdownFile
import com.juzabel.mdfinder.model.ChatMessage
import com.juzabel.mdfinder.tool.MarkdownSearchTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun koogRunAgent(
    query: String,
    files: List<MarkdownFile>,
    conversationHistory: List<ChatMessage>,
    apiKeyOrEndpoint: String,
    modelTag: String,
    isGemini: Boolean
): String = withContext(Dispatchers.IO) {
    val toolRegistry = ToolRegistry {
        tool(MarkdownSearchTool(files))
        tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
        tool(ListDirectoryTool(JVMFileSystemProvider.ReadOnly))
    }

    val installDebugEvents: GraphAIAgent.FeatureContext.() -> Unit = {
        handleEvents {
            onToolCallStarting { ctx ->
                println("[Koog/Agent/Tool→] ${ctx.toolName} args=${ctx.toolArgs}")
            }
            onToolCallCompleted { ctx ->
                val preview = ctx.toolResult?.toString()?.take(200)?.replace("\n", " ")
                println("[Koog/Agent/Tool←] ${ctx.toolName} result=$preview")
            }
            onToolCallFailed { ctx ->
                println("[Koog/Agent/Tool✗] ${ctx.toolName} msg=${ctx.message} error=${ctx.error}")
            }
            onToolValidationFailed { ctx ->
                println("[Koog/Agent/Tool✗val] ${ctx.toolName} msg=${ctx.message} error=${ctx.error}")
            }
            onLLMCallStarting { ctx ->
                println("[Koog/Agent/LLM→] prompt messages=${ctx.prompt.messages.size} tools=${ctx.tools.size}")
            }
            onLLMCallCompleted { ctx ->
                val kind = ctx.response?.let { it::class.simpleName } ?: "none"
                println("[Koog/Agent/LLM←] response=$kind")
            }
            onAgentExecutionFailed { ctx ->
                println("[Koog/Agent/✗] ${ctx.error::class.simpleName}: ${ctx.error.message}")
            }
        }
    }

    val systemPrompt = """
      You are a note‑only answerer. You have access to these tools:

        1. `markdown_search` – searches the user's indexed markdown notes for a topic, keyword, or phrase.
           It returns matching files with relevant excerpts (including line numbers and surrounding context).
           The excerpts contain the actual file content — you can answer directly from them.
           Use short, focused keywords (1‑3 words) for the query parameter.
        
        2. `__read_file__` – reads the full content of a file when given its absolute path.
           Use this only if you need the complete file beyond the excerpts returned by `markdown_search`.
        
        3. `list_directory` – lists files in a directory. Rarely needed.
        
        Your task: answer the user's question or search request using **only** the markdown files indexed by `markdown_search`.
        
        Process:
        
        - Step 1: Call `markdown_search` with a short, focused query (1‑3 keywords that capture the user's topic).
          This gives you matching files with relevant excerpts.
        
        - Step 2: If you need more context from a specific file, call `__read_file__` with its absolute path.
        
        - Step 3: Synthesize an answer **using only** the content from the files.
          Do NOT add any information from your own training data, common sense, or outside sources.
        
        - Step 4: If none of the files contain relevant information, respond:
          "The markdown files contain no information about [topic]."
        
        - Step 5: Always end your answer with the names of the specific files you used, e.g.:
          "Sources: file1.md, file2.md"
        
        Important rules:
        - Never guess or invent information.
        - If a file's content is incomplete or ambiguous, say so rather than filling gaps.
        - If `markdown_search` returns no files, stop and report that no files match the topic.
        - If you do not find anything about the topic in the markdown files, do not provide information using your knowledge — show a message about the lack of info in the files.
        
        User's request: {{user_query}}
    """.trimIndent()

    val userMessage = "Question: ${query.trim()}"

    if (isGemini) {
        println("[Koog/Agent/Gemini] Running agent with ${files.size} files, query=$query")
        val executor = simpleGoogleAIExecutor(apiKeyOrEndpoint)
        val config = AIAgentConfig.withSystemPrompt(
            prompt = systemPrompt, llm = GoogleModels.Gemini2_5Flash, maxAgentIterations = 20
        )
        val agent = GraphAIAgent(
            promptExecutor = executor,
            agentConfig = config,
            strategy = singleRunStrategy(),
            toolRegistry = toolRegistry,
            installFeatures = installDebugEvents
        )
        val result = agent.run(userMessage)
        println("[Koog/Agent/Gemini] result length=${result.length} preview=${result.take(200).replace("\n", " ")}")
        result.ifBlank { throw Exception("Agent returned an empty response. The model may have exceeded its context window or failed to produce a reply.") }
    } else {
        println("[Koog/Agent/Ollama] Running direct prompt with ${files.size} files, model=$modelTag, query=$query")

        // Pre-search files using MarkdownSearchTool directly (in Kotlin, not via model)
        val searchTool = MarkdownSearchTool(files)
        val searchResult = searchTool.execute(
            MarkdownSearchTool.Args(query = query, maxFiles = 5, contextLines = 3)
        )

        // Build complete prompt with search results embedded inline
        val fullPrompt = """
            You are a note-only answerer. Answer the user's question using ONLY the markdown file excerpts below.

            Search results from the user's markdown files:

            $searchResult

            Instructions:
            - Synthesize an answer using ONLY the content from the excerpts above.
            - Do NOT add any information from your own training data, common sense, or outside sources.
            - If the excerpts don't contain relevant information, respond: "The markdown files contain no information about [topic]."
            - Always end your answer with: "Sources: file1.md, file2.md" listing only the files whose content you actually used.
            - Never guess or invent information.

            User's question: $query
        """.trimIndent()

        // Direct LLM call — no agent loop, single prompt → single response
        val ollamaModel = LLModel(
            provider = LLMProvider.Ollama,
            id = modelTag,
            capabilities = listOf(LLMCapability.Completion, LLMCapability.Temperature)
        )

        val client = OllamaClient(baseUrl = apiKeyOrEndpoint)
        val prompt = Prompt.build("ollama-direct") { user(fullPrompt) }
        val response = client.execute(prompt, ollamaModel)
        val result = response.textContent()

        println("[Koog/Agent/Ollama] result length=${result.length} preview=${result.take(200).replace("\n", " ")}")
        result.ifBlank {
            throw Exception("Ollama returned an empty response. The model may have exceeded its context window.")
        }
    }
}
