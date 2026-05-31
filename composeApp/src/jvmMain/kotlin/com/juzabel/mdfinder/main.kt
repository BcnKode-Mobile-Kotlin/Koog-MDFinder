package com.juzabel.mdfinder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.juzabel.mdfinder.ui.components.CustomTitleBar
import com.juzabel.mdfinder.ui.screens.ChatScreen
import com.juzabel.mdfinder.viewmodel.ChatViewModel

fun main() = application {
    val windowState = rememberWindowState()
    Window(
        onCloseRequest = ::exitApplication,
        title = "MDFinder",
        undecorated = true,
        state = windowState,
    ) {
        val viewModel = remember { ChatViewModel() }
        val isDarkTheme by viewModel.isDarkTheme.collectAsState()

        MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CustomTitleBar(
                        title = "MDFinder",
                        window = window,
                        windowState = windowState,
                        onCloseRequest = ::exitApplication
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ChatScreen(viewModel)
                    }
                }
            }
        }
    }
}
