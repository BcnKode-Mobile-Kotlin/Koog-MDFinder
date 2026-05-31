package com.juzabel.mdfinder.viewmodel

import androidx.compose.runtime.Stable
import com.juzabel.mdfinder.data.MarkdownFile
import com.juzabel.mdfinder.data.createMarkdownFileScanner
import com.juzabel.mdfinder.model.AIModel
import com.juzabel.mdfinder.model.ChatMessage
import com.juzabel.mdfinder.model.ChatRole
import com.juzabel.mdfinder.model.SettingsData
import com.juzabel.mdfinder.platform.getCurrentTimeMillis
import com.juzabel.mdfinder.platform.openFolderPicker
import com.juzabel.mdfinder.repository.AgentRepository
import com.juzabel.mdfinder.repository.OllamaHealthCheck
import com.juzabel.mdfinder.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

@Stable
class ChatViewModel {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _selectedFolderPath = MutableStateFlow<String?>(null)
    val selectedFolderPath: StateFlow<String?> = _selectedFolderPath.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isChatEnabled = MutableStateFlow(false)
    val isChatEnabled: StateFlow<Boolean> = _isChatEnabled.asStateFlow()

    private val _skippedFileCount = MutableStateFlow(0)
    val skippedFileCount: StateFlow<Int> = _skippedFileCount.asStateFlow()

    // Phase 2: Model Selection
    private val _selectedModel = MutableStateFlow<AIModel?>(null)
    val selectedModel: StateFlow<AIModel?> = _selectedModel.asStateFlow()

    // Phase 2: Model Availability Status
    private val _modelAvailability = MutableStateFlow<Map<AIModel, Boolean>>(
        mapOf(
            AIModel.GEMMA4 to false,
            AIModel.GEMINI to true  // Always available in Phase 2
        )
    )
    val modelAvailability: StateFlow<Map<AIModel, Boolean>> = _modelAvailability.asStateFlow()

    // Phase 2: Error Message State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Phase 3: Settings State
    private val _settingsData = MutableStateFlow<SettingsData>(SettingsData())
    val settingsData: StateFlow<SettingsData> = _settingsData.asStateFlow()

    private val _showSettingsModal = MutableStateFlow(false)
    val showSettingsModal: StateFlow<Boolean> = _showSettingsModal.asStateFlow()

    private val _ollamaHealthStatus = MutableStateFlow(false)
    val ollamaHealthStatus: StateFlow<Boolean> = _ollamaHealthStatus.asStateFlow()

    // Phase 4: Copy Toast State
    private val _showCopyToast = MutableStateFlow(false)
    val showCopyToast: StateFlow<Boolean> = _showCopyToast.asStateFlow()

    private val _toastMessage = MutableStateFlow("")
    val toastMessage: StateFlow<String> = _toastMessage.asStateFlow()

    // Phase 4: Error Banner State
    private val _errorBannerMessage = MutableStateFlow<String?>(null)
    val errorBannerMessage: StateFlow<String?> = _errorBannerMessage.asStateFlow()

    // Theme
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private var cachedFiles: List<MarkdownFile> = emptyList()

    private val fileScanner = createMarkdownFileScanner()
    private val agentRepository = AgentRepository()
    private val healthCheck = OllamaHealthCheck()
    private val settingsRepository = SettingsRepository()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        // Load settings on startup (Phase 3, D-14)
        viewModelScope.launch {
            val settings = settingsRepository.loadSettings()
            _settingsData.value = settings

            // Restore model selection first so scanFolder sees it when setting _isChatEnabled
            if (!settings.selectedModel.isNullOrEmpty()) {
                val model = AIModel.values().find { it.modelId == settings.selectedModel }
                if (model != null) {
                    _selectedModel.value = model
                }
            }

            // Run Ollama health check with configured endpoint
            val configuredHealthCheck = OllamaHealthCheck(settings.ollamaEndpoint)
            val isHealthy = configuredHealthCheck.checkHealth()
            _ollamaHealthStatus.value = isHealthy

            // Update availability map with health check result
            _modelAvailability.value = mapOf(
                AIModel.GEMMA4 to isHealthy,
                AIModel.GEMINI to true
            )

            // Restore theme preference
            _isDarkTheme.value = settings.isDarkTheme

            // Restore folder path and re-scan so cachedFiles is populated and chat is enabled
            // Done last so _selectedModel is already set when scan completes
            if (!settings.folderPath.isNullOrEmpty()) {
                scanFolder(settings.folderPath)
            }

            println("[ChatViewModel] Settings loaded: folder=${settings.folderPath}, model=${settings.selectedModel}")
        }
    }

    fun submitMessage(userQuery: String) {
        if (userQuery.isBlank() || cachedFiles.isEmpty()) return

        // Add user message to history
        val userMessage = ChatMessage(
            id = generateId(),
            role = ChatRole.USER,
            content = userQuery.trim(),
            timestamp = getCurrentTimeMillis()
        )
        _messages.value = _messages.value + userMessage

        // Clear input text
        _inputText.value = ""

        // Set loading state
        _isLoading.value = true

        // Query agent with file contents, conversation history, selected model, and settings
        viewModelScope.launch(Dispatchers.Main) {
            val selectedModelValue = _selectedModel.value ?: AIModel.GEMINI
            try {
                val response = agentRepository.queryAgent(
                    query = userQuery.trim(),
                    files = cachedFiles,
                    conversationHistory = _messages.value,
                    model = selectedModelValue,
                    settingsData = _settingsData.value
                )

                if (response.isError) {
                    val errMsg = response.errorMessage ?: ""
                    when (selectedModelValue) {
                        AIModel.GEMMA4-> setErrorMessage(
                            banner = "Ollama error. Check endpoint and model availability.",
                            chatMessage = "Ollama error.\n\n$errMsg"
                        )
                        AIModel.GEMINI -> setErrorMessage(
                            banner = "Gemini API error. Check your API key and internet connection.",
                            chatMessage = "I couldn't connect to Gemini. Check your API key in Settings.\n\nError: $errMsg"
                        )
                    }
                    _isLoading.value = false
                } else {
                    // Agent done — stop loading indicator and start streaming the reply
                    val msgId = generateId()
                    val streamingMsg = ChatMessage(
                        id = msgId,
                        role = ChatRole.ASSISTANT,
                        content = "",
                        timestamp = getCurrentTimeMillis(),
                        isStreaming = true
                    )
                    _messages.value = _messages.value + streamingMsg
                    _isLoading.value = false

                    streamText(response.summary).collect { chunk ->
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == msgId) msg.copy(content = msg.content + chunk) else msg
                        }
                    }

                    // Mark streaming complete so cursor disappears and copy button appears
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == msgId) msg.copy(isStreaming = false) else msg
                    }
                }
            } catch (e: Exception) {
                val errMsg = e.message.orEmpty()
                when (selectedModelValue) {
                    AIModel.GEMMA4 -> setErrorMessage(
                        banner = "Ollama error. Check endpoint and model availability.",
                        chatMessage = "Ollama error.\n\n$errMsg"
                    )
                    AIModel.GEMINI -> setErrorMessage(
                        banner = "Gemini API error. Check your API key and internet connection.",
                        chatMessage = "I couldn't connect to Gemini. Check your API key in Settings.\n\nError: $errMsg"
                    )
                }
                _isLoading.value = false
            }
        }
    }

    suspend fun selectFolderInteractive() {
        val selectedPath = openFolderPicker() ?: return  // User cancelled
        setSelectedFolder(selectedPath)
    }

    private fun scanFolder(folderPath: String) {
        _selectedFolderPath.value = folderPath
        _isLoading.value = true
        _isChatEnabled.value = false

        // Scan folder asynchronously
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val result = fileScanner.scanFolder(folderPath)

                if (result.errorMessage != null) {
                    // Error occurred during scanning
                    _selectedFolderPath.value = null
                    _isChatEnabled.value = false
                } else if (result.files.isEmpty()) {
                    // No markdown files found
                    _selectedFolderPath.value = null
                    _isChatEnabled.value = false
                } else {
                    // Files found, enable chat only if BOTH folder AND model selected
                    cachedFiles = result.files
                    _skippedFileCount.value = result.skippedCount
                    _isChatEnabled.value = _selectedModel.value != null
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSelectedFolder(path: String) {
        viewModelScope.launch {
            _selectedFolderPath.value = path
            _messages.value = emptyList()  // Clear history (Phase 1 pattern)

            // Update settings and save
            val selectedModelId = _selectedModel.value?.modelId
            val updatedSettings = _settingsData.value.copy(folderPath = path, selectedModel = selectedModelId)
            val saved = settingsRepository.saveSettings(updatedSettings)
            if (saved) {
                _settingsData.value = updatedSettings
            }

            // Now scan the folder
            scanFolder(path)
        }
    }

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun selectModel(model: AIModel) {
        // Clear chat history when model changes (D-03 from Phase 2 context)
        _messages.value = emptyList()

        if (model.isLocal) {
            // For local models, perform health check to validate availability
            viewModelScope.launch {
                // Use configured Ollama endpoint from settings
                val ollamaAvailable = healthCheck.checkHealth(_settingsData.value.ollamaEndpoint)

                // Update availability for this model
                _modelAvailability.value = _modelAvailability.value.toMutableMap().apply {
                    put(AIModel.GEMMA4, ollamaAvailable)
                }

                if (ollamaAvailable) {
                    // Ollama is running, enable the model
                    _selectedModel.value = model
                    _errorMessage.value = null
                    _isChatEnabled.value = _selectedFolderPath.value != null

                    // Save model selection to settings
                    val updatedSettings = _settingsData.value.copy(selectedModel = model.modelId)
                    settingsRepository.saveSettings(updatedSettings)
                    _settingsData.value = updatedSettings
                } else {
                    // Ollama is not running, show error and disable chat
                    _selectedModel.value = null
                    _errorMessage.value = "Ollama not running. Start Ollama or switch to Gemini to continue."
                    _isChatEnabled.value = false
                }
            }
        } else {
            // For cloud models (Gemini), no health check needed
            _selectedModel.value = model
            _errorMessage.value = null
            _isChatEnabled.value = _selectedFolderPath.value != null

            // Save model selection to settings (Phase 3, D-16, D-17)
            viewModelScope.launch {
                val updatedSettings = _settingsData.value.copy(selectedModel = model.modelId)
                settingsRepository.saveSettings(updatedSettings)
                _settingsData.value = updatedSettings
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    fun clearChatHistory() {
        // Clear messages only, preserve settings (per D-09)
        _messages.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateSettingsData(updatedSettings: SettingsData) {
        viewModelScope.launch {
            // Save immediately — do not block on network validation.
            // Bad API keys / unreachable endpoints surface as errors when actually used.
            val saved = settingsRepository.saveSettings(updatedSettings)
            if (saved) {
                _settingsData.value = updatedSettings

                // Update ViewModel state from new settings
                if (!updatedSettings.folderPath.isNullOrEmpty()) {
                    _selectedFolderPath.value = updatedSettings.folderPath
                }
                if (!updatedSettings.selectedModel.isNullOrEmpty()) {
                    val model = AIModel.values().find { it.modelId == updatedSettings.selectedModel }
                    if (model != null) {
                        _selectedModel.value = model
                    }
                }

                // Re-run Ollama health check with new endpoint
                val configuredHealthCheck = OllamaHealthCheck(updatedSettings.ollamaEndpoint)
                val isHealthy = configuredHealthCheck.checkHealth()
                _ollamaHealthStatus.value = isHealthy
            }
        }
    }

    fun setShowSettingsModal(show: Boolean) {
        _showSettingsModal.value = show
    }

    // Function to copy to clipboard and show toast
    fun copyToClipboard(text: String) {
        // Actual clipboard copy happens in ChatScreen (platform-specific via expect/actual)
        // ViewModel just manages the toast state
        viewModelScope.launch {
            _toastMessage.value = "Copied!"
            _showCopyToast.value = true

            // Auto-dismiss after 2 seconds
            delay(2000)
            _showCopyToast.value = false
            _toastMessage.value = ""
        }
    }

    // Function to set error banner and also add to chat
    fun setErrorMessage(banner: String, chatMessage: String) {
        _errorBannerMessage.value = banner

        // Also add error to conversation history
        val errorChatMsg = ChatMessage(
            id = generateId(),
            role = ChatRole.ASSISTANT,
            content = chatMessage,
            timestamp = getCurrentTimeMillis()
        )
        _messages.value = _messages.value + errorChatMsg
    }

    // Function to clear error banner (user can dismiss)
    fun clearErrorBanner() {
        _errorBannerMessage.value = null
    }

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        viewModelScope.launch {
            val updatedSettings = _settingsData.value.copy(isDarkTheme = newValue)
            settingsRepository.saveSettings(updatedSettings)
            _settingsData.value = updatedSettings
        }
    }

    private fun streamText(text: String): Flow<String> = flow {
        var index = 0
        while (index < text.length) {
            val chunkSize = if (text[index] == '\n') 1 else 3
            val end = minOf(index + chunkSize, text.length)
            emit(text.substring(index, end))
            index = end
            delay(15)
        }
    }

    private fun generateId(): String {
        return "msg_${Random.nextLong()}"
    }
}
