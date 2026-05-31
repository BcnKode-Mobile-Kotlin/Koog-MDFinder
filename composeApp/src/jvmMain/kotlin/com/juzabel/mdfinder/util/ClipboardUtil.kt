package com.juzabel.mdfinder.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun copyToSystemClipboard(text: String) {
    val toolkit = Toolkit.getDefaultToolkit()
    val clipboard = toolkit.systemClipboard
    val stringSelection = StringSelection(text)
    clipboard.setContents(stringSelection, null)
}
