# MDFinder

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.3.20" />
  <img src="https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose Multiplatform 1.10.3" />
  <img src="https://img.shields.io/badge/Gradle-9.0-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle 9.0" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-D22128?style=for-the-badge&logo=apache&logoColor=white" alt="Apache 2.0 License" />
  <img src="https://img.shields.io/badge/Platform-JVM-6B46C1?style=for-the-badge" alt="Platform" />
</p>

<p align="center">
  <strong>AI-powered chat for your local Markdown knowledge base.</strong><br />
  Ask natural-language questions about your notes — answers synthesized exclusively from your indexed content.
</p>

---

## Overview

MDFinder is a **Kotlin Multiplatform (KMP)** desktop application that implements **Retrieval-Augmented Generation (RAG)** for personal Markdown notes. Select a folder of `.md` files, then ask questions in natural language. The app uses an LLM agent — powered by the [Koog](https://github.com/kotlindev/koog) agent framework — to search your notes and synthesize answers grounded in your indexed content.

### How It Works

```
You → Natural-language question → Koog Agent
                                      ├─ MarkdownSearchTool    (keyword search)
                                      ├─ ReadFileTool          (read specific files)
                                      └─ ListDirectoryTool     (browse folder structure)
                                              ↓
                                    LLM synthesizes answer from
                                    retrieved Markdown context
                                              ↓
                                    Streaming response → Chat UI
```

The agent is given tools to navigate and search your markdown directory. Every answer is backed by citations from your files — the model never hallucinates outside the provided context.

---

## Features

### Core
- **Folder-based Markdown Indexing** — Select any directory; all `.md` files are discovered and searchable
- **Keyword Search with Stop-Word Filtering** — Fast, relevance-ranked search across your notes
- **Dual AI Model Support** — Choose between cloud and local inference at any time
- **Streaming Chat UI** — Responses appear token-by-token with an animated typing cursor
- **Settings Management** — Persistent configuration modal for model selection, API keys, theme, and more

### AI Models

| Model            | Type  | Provider                    | Setup                                                                |
| ---------------- | ----- | --------------------------- | -------------------------------------------------------------------- |
| **Gemini 2.5 Flash** | Cloud | Google AI                   | Requires API key from [AI Studio](https://aistudio.google.com/apikey) |
| **Gemma4**           | Local | [Ollama](https://ollama.com) | Requires Ollama installed + `gemma4:e2b` pulled                        |

### Chat Experience
- User/assistant message bubbles with distinct styling
- Animated typing cursor during streamed responses
- One-click copy button on each assistant message
- Error banners for failed queries or configuration issues
- Toast notifications for transient status updates
- Dark and light theme support, persisted across sessions

### Cross-Platform
- **JVM Desktop** — Primary target, packaged as DMG, MSI, or DEB

---

## Tech Stack

| Layer           | Technology                 | Version            |
| --------------- | -------------------------- | ------------------ |
| **Language**        | Kotlin                     | 2.3.20             |
| **UI Framework**    | Compose Multiplatform      | 1.10.3             |
| **Build System**    | Gradle                     | 9.0                |
| **Agent Framework** | Koog Agents + Ext          | 1.0.0 / 1.0.0-beta |
| **HTTP Client**     | Ktor Client (CIO engine)   | 3.2.2              |
| **Serialization**   | Kotlinx Serialization JSON | 1.7.3              |
| **Concurrency**     | Kotlinx Coroutines         | 1.10.2             |
| **License**         | Apache 2.0                 | —                  |

---

## Prerequisites

### Required
- **JDK 17+** — [Adoptium](https://adoptium.net/) or any compatible distribution

### Model-specific

**Gemini (cloud):**
1. Get an API key at [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
2. Enter the key in the app's Settings modal

**Gemma4 (local):**
1. Install [Ollama](https://ollama.com)
2. Pull the model:
   ```bash
   ollama pull gemma4:e2b
   ```
3. Ensure Ollama is running (`ollama serve` starts automatically on install)

---

## Quick Start

### Run the Desktop App

```bash
./gradlew :composeApp:run
```

This compiles and launches the desktop JVM application. On first run, Gradle will download all dependencies.

### Build Desktop Distributions

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

Creates a native package for your operating system:
- **macOS** → `.dmg` disk image in `composeApp/build/compose/binaries/main/dmg/`
- **Windows** → `.msi` installer
- **Linux** → `.deb` package

---

## Usage

1. **Launch** the app (`./gradlew :composeApp:run`)
2. **Select a folder** containing markdown files via the "Choose Folder" button — use the included `ProgrammingLanguages/` directory to test
3. **Choose your model** in Settings:
   - *Gemini* — cloud inference (requires API key)
   - *Gemma4* — local inference via Ollama
4. **Configure**:
   - For Gemini: enter your Google API key in Settings
   - For Gemma4: set the Ollama endpoint (default: `http://localhost:11434`)
5. **Ask questions** about your markdown content

### Testing with Sample Data

The repository includes `ProgrammingLanguages/` — four Spanish-language markdown files covering programming language origins, creators, examples, and usage. Try questions like:

- *"¿Quién creó Python?"* (Who created Python?)
- *"Dame ejemplos de código en JavaScript"* (Give me code examples in JavaScript)
- *"¿Para qué se usa Rust?"* (What is Rust used for?)

---

## Project Structure

```
MDFinder/
├── build.gradle.kts                  # Root Gradle build
├── settings.gradle.kts               # Project settings & repository config
├── gradle.properties                 # JVM args, Kotlin/Android flags
├── gradle/
│   └── libs.versions.toml            # Version catalog (all dependencies)
├── LICENSE                           # Apache 2.0
├── ProgrammingLanguages/             # Sample markdown data (4 files)
│   ├── Creadores.md
│   ├── Ejemplos.md
│   ├── Origen.md
│   └── Uso.md
├── composeApp/
│   ├── build.gradle.kts               # Module build — KMP targets & dependencies
│   └── src/
│       ├── commonMain/kotlin/com/juzabel/mdfinder/
│       │   ├── App.kt                 # Root composable — theme + ChatScreen
│       │   ├── Platform.kt            # expect declarations (platform API)
│       │   ├── agent/                 # LLM agent layer
│       │   │   ├── ModelExecutor.kt   # Abstract model execution interface
│       │   │   ├── KoogLLMExecutor.kt # Koog agent executor (tool-driven)
│       │   │   └── LLMClientFactory.kt# Gemini & Ollama client factory
│       │   ├── data/                  # File scanning & indexing
│       │   ├── model/                 # Domain models
│       │   │   ├── AIModel.kt         # Model enum (Gemini, Gemma4)
│       │   │   ├── ChatMessage.kt     # User/assistant message types
│       │   │   └── SettingsData.kt    # Persisted settings data class
│       │   ├── repository/            # Data access & business logic
│       │   │   ├── AgentRepository.kt # Agent execution orchestration
│       │   │   ├── SettingsRepository.kt# Settings persistence
│       │   │   ├── OllamaHealthCheck.kt# Ollama connectivity check
│       │   │   └── SettingsValidator.kt# Input validation
│       │   ├── ui/                    # Compose UI layer
│       │   │   ├── screens/           # ChatScreen (main screen)
│       │   │   └── components/        # Bubbles, modals, buttons, toast
│       │   ├── viewmodel/
│       │   │   └── ChatViewModel.kt   # Central state — single ViewModel
│       │   ├── util/                  # Utility functions
│       │   └── platform/              # expect declarations (file I/O, etc.)
│       └── jvmMain/                   # Desktop implementations
```

---

## Architecture

MDFinder follows an **MVVM (Model-View-ViewModel)** architecture with Kotlin `StateFlow` for reactive state management.

```
┌──────────────────────────────────────────────────────────┐
│  UI Layer (Compose)                                       │
│  ChatScreen → SettingsModal → MessageBubbles → Toast      │
│       ↑ collects StateFlow                                │
├──────────────────────────────────────────────────────────┤
│  ViewModel Layer                                          │
│  ChatViewModel (single source of truth)                   │
│  Manages: messages, settings, loading, errors             │
│       ↑ calls                                             │
├──────────────────────────────────────────────────────────┤
│  Repository Layer                                         │
│  AgentRepository — orchestrates agent runs                │
│  SettingsRepository — persistence & validation            │
│  OllamaHealthCheck — connectivity verification            │
│       ↑ uses                                              │
├──────────────────────────────────────────────────────────┤
│  Agent Layer                                              │
│  KoogLLMExecutor — Koog agent with tools                  │
│  LLMClientFactory — Gemini & Ollama client creation       │
│  Tools: MarkdownSearchTool, ReadFileTool, ListDirectoryTool│
│       ↑ talks to                                          │
├──────────────────────────────────────────────────────────┤
│  External                                                │
│  Google Gemini API (cloud) / Ollama (localhost:11434)     │
└──────────────────────────────────────────────────────────┘
```

### Key Design Decisions

- **Single ViewModel** — `ChatViewModel` owns all state. No fragmented state across screens.
- **Expect/Actual Pattern** — Platform-specific code (file I/O, clipboard, folder picker, settings storage) is abstracted behind `expect` declarations in `commonMain`, with `actual` implementations in `jvmMain`.
- **Tool-Driven Agent** — The Koog agent receives tools that operate on the user's filesystem. The agent decides which tool to use and when, creating a feedback loop of search → read → synthesize.
- **Streaming by Default** — Responses are streamed token-by-token from the LLM through the ViewModel to the Compose UI, providing immediate visual feedback.

---

## Configuration

MDFinder stores settings persistently. The following can be configured through the Settings modal (gear icon in the chat interface):

| Setting         | Description                          | Default                 |
| --------------- | ------------------------------------ | ----------------------- |
| **Folder Path**     | Directory of markdown files to index | None — must be selected |
| **Model**           | AI model to use                      | Gemini 2.5 Flash        |
| **Gemini API Key**  | Google AI Studio API key             | None                    |
| **Ollama Endpoint** | URL of the Ollama server             | `http://localhost:11434`  |
| **Theme**           | Dark or light color scheme           | System default          |
| **Clear History**   | Reset the chat conversation          | —                       |

---

## License

MDFinder is open-source software licensed under the [Apache License 2.0](LICENSE).

---

## Acknowledgements

- [Koog Agents](https://github.com/kotlindev/koog) — Kotlin-native LLM agent framework
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) — Declarative UI across platforms
- [Google Gemini](https://ai.google.dev/) — Cloud AI model
- [Ollama](https://ollama.com) — Local LLM inference
- [Gemma](https://ai.google.dev/gemma) — Open-weights model from Google
