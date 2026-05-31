package com.juzabel.mdfinder.tool

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.juzabel.mdfinder.data.MarkdownFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Self-contained search tool: reads the indexed markdown files, looks for the query
 * inside their content, and returns the matching files together with the relevant
 * excerpts (with surrounding context lines).
 *
 * Unlike a pure index tool, this returns actual content, so the agent does NOT need
 * to make a follow-up __read_file__ call to answer.
 */
class MarkdownSearchTool(
    private val files: List<MarkdownFile>
) : SimpleTool<MarkdownSearchTool.Args>(
    argsType = typeToken<Args>(),
    name = "markdown_search",
    description = "Search the user's indexed markdown notes for a topic, keyword, or phrase. " +
            "Returns the matching files and the relevant excerpts (with line numbers and surrounding " +
            "context) so you can answer directly. This tool reads file content itself — you do NOT " +
            "need to read the files separately."
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "The topic, keyword, or phrase to search for inside the notes. Case-insensitive. Required."
        )
        val query: String,
        @property:LLMDescription(
            "Maximum number of matching files to return (most relevant first). Defaults to 10."
        )
        val maxFiles: Int = 10,
        @property:LLMDescription(
            "Number of context lines to include before and after each matching line. Defaults to 2."
        )
        val contextLines: Int = 2
    )

    private data class FileMatch(
        val file: MarkdownFile,
        val hitCount: Int,
        val snippets: List<String>
    )

    companion object {
        private val STOP_WORDS = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "shall", "to", "of", "in", "for",
            "on", "with", "at", "by", "from", "as", "into", "through", "during",
            "before", "after", "above", "below", "between", "under", "again",
            "further", "then", "once", "here", "there", "when", "where", "why",
            "how", "all", "each", "every", "both", "few", "more", "most", "other",
            "some", "such", "no", "nor", "not", "only", "own", "same", "so",
            "than", "too", "very", "just", "because", "but", "and", "or", "if",
            "while", "this", "that", "these", "those", "what", "which", "who",
            "whom", "my", "your", "his", "her", "its", "our", "their", "me",
            "him", "us", "them", "i", "you", "he", "she", "it", "we", "they",
            "about", "up", "out", "off", "over", "also", "any", "much", "many"
        )
    }

    override suspend fun execute(args: Args): String = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext "No markdown files are indexed."

        val query = args.query.trim()
        if (query.isEmpty()) return@withContext "Please provide a non-empty search query."

        // Tokenize query into individual words, filter stop words
        val queryWords = query.lowercase()
            .split(Regex("\\s+"))
            .map { it.trim('.', ',', '?', '!', ':', ';', '"', '\'', '(', ')', '[', ']', '{', '}') }
            .filter { it.length > 1 && it !in STOP_WORDS }
            .distinct()
            .take(10)

        if (queryWords.isEmpty()) return@withContext "No searchable keywords found in \"$query\"."

        val matches = files.asSequence()
            .mapNotNull { file ->
                val content = runCatching { File(file.path).readText() }.getOrNull()
                    ?: return@mapNotNull null

                val lines = content.lines()

                // Find lines matching ANY query word
                val hitIndices = lines.indices.filter { lineIdx ->
                    val lineLower = lines[lineIdx].lowercase()
                    queryWords.any { word -> lineLower.contains(word) }
                }
                if (hitIndices.isEmpty()) return@mapNotNull null

                // Count total word matches for ranking
                val totalHits = hitIndices.sumOf { lineIdx ->
                    val lineLower = lines[lineIdx].lowercase()
                    queryWords.count { word -> lineLower.contains(word) }
                }

                FileMatch(
                    file = file,
                    hitCount = totalHits,
                    snippets = buildSnippets(lines, hitIndices, args.contextLines.coerceAtLeast(0))
                )
            }
            .sortedByDescending { it.hitCount }
            .take(args.maxFiles.coerceAtLeast(1))
            .toList()

        if (matches.isEmpty()) {
            return@withContext "No markdown files contain keywords matching \"$query\"."
        }

        buildString {
            appendLine("Found keywords from \"$query\" in ${matches.size} file(s):")
            appendLine()
            matches.forEach { m ->
                appendLine("## ${m.file.name}")
                appendLine("- Path: `${m.file.path}`")
                appendLine("- Size: ${formatSize(m.file.sizeBytes)}")
                appendLine("- Matches: ${m.hitCount}")
                appendLine()
                m.snippets.forEach { snippet ->
                    appendLine("```markdown")
                    appendLine(snippet)
                    appendLine("```")
                    appendLine()
                }
            }
        }.trim()
    }

    /**
     * Turns the matching line indices into readable, line-numbered snippets, merging
     * overlapping/adjacent context windows so the same lines aren't repeated.
     */
    private fun buildSnippets(
        lines: List<String>,
        hitIndices: List<Int>,
        contextLines: Int
    ): List<String> {
        val windows = hitIndices.map { idx ->
            (idx - contextLines).coerceAtLeast(0)..(idx + contextLines).coerceAtMost(lines.lastIndex)
        }

        val merged = mutableListOf<IntRange>()
        for (w in windows) {
            val last = merged.lastOrNull()
            if (last != null && w.first <= last.last + 1) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, w.last)
            } else {
                merged += w
            }
        }

        return merged.map { range ->
            range.joinToString("\n") { i -> "${i + 1}: ${lines[i]}" }
        }
    }

    // Human-readable file sizes
    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}