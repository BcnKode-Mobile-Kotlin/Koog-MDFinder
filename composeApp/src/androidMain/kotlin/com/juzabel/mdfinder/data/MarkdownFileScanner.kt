package com.juzabel.mdfinder.data

actual fun createMarkdownFileScanner(): IMarkdownFileScanner = AndroidMarkdownFileScannerStub()

private class AndroidMarkdownFileScannerStub : IMarkdownFileScanner {
    override suspend fun scanFolder(folderPath: String): ScanResultData {
        // TODO: Implement Android file scanner using ContentProvider
        return ScanResultData(
            files = emptyList(),
            skippedCount = 0,
            errorMessage = "File scanning not yet implemented on Android"
        )
    }
}
