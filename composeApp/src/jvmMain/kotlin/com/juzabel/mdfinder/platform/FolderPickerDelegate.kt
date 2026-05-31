package com.juzabel.mdfinder.platform

import java.io.File
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun openFolderPicker(): String? {
    return withContext(Dispatchers.IO) {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fileChooser.dialogTitle = "Select Markdown Folder"
        fileChooser.approveButtonText = "Select"

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile?.absolutePath
        } else {
            null
        }
    }
}
