package com.juzabel.mdfinder.data

actual fun createMarkdownFileScanner(): IMarkdownFileScanner = IOSMarkdownFileScannerStub()

private class IOSMarkdownFileScannerStub : IMarkdownFileScanner {
    override suspend fun scanFolder(folderPath: String): ScanResultData {
        // TODO: Implement iOS file scanner using FilesProvider
        return ScanResultData(
            files = emptyList(),
            skippedCount = 0,
            errorMessage = "File scanning not yet implemented on iOS"
        )
    }
}
