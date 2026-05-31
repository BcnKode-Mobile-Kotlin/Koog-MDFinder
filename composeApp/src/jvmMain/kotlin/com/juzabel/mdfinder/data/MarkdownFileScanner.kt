package com.juzabel.mdfinder.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MarkdownFileScanner : IMarkdownFileScanner {
    companion object {
        private const val MAX_FILE_SIZE_BYTES = 512 * 1024L // 512 KB
        private const val MAX_FILE_COUNT = 500
    }

    override suspend fun scanFolder(folderPath: String): ScanResultData = withContext(Dispatchers.IO) {
        return@withContext try {
            val folder = File(folderPath)

            if (!folder.exists() || !folder.isDirectory) {
                return@withContext ScanResultData(
                    files = emptyList(),
                    skippedCount = 0,
                    errorMessage = "Folder not found or is not a directory"
                )
            }

            val files = mutableListOf<MarkdownFile>()
            var skippedCount = 0

            folder.walk().forEach { file ->
                // Check if we've reached max file count
                if (files.size >= MAX_FILE_COUNT) {
                    return@forEach
                }

                // Only process markdown files (case-insensitive)
                if (!file.isFile || !file.name.lowercase().endsWith(".md")) {
                    return@forEach
                }

                try {
                    // Check file size
                    if (file.length() > MAX_FILE_SIZE_BYTES) {
                        skippedCount++
                        return@forEach
                    }

                    // Normalize path to forward slashes
                    val normalizedPath = file.absolutePath.replace("\\", "/")

                    // The AI agent reads files on demand via Koog's __read_file__ tool.
                    files.add(
                        MarkdownFile(
                            path = normalizedPath,
                            name = file.name,
                            sizeBytes = file.length()
                        )
                    )
                } catch (e: Exception) {
                    skippedCount++
                }
            }

            ScanResultData(
                files = files,
                skippedCount = skippedCount,
                errorMessage = null
            )
        } catch (e: Exception) {
            ScanResultData(
                files = emptyList(),
                skippedCount = 0,
                errorMessage = "Failed to scan folder: ${e.message}"
            )
        }
    }
}

actual fun createMarkdownFileScanner(): IMarkdownFileScanner = MarkdownFileScanner()
