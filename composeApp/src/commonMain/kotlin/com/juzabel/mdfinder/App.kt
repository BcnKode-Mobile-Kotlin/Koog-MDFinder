package com.juzabel.mdfinder

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.juzabel.mdfinder.ui.screens.ChatScreen
import com.juzabel.mdfinder.viewmodel.ChatViewModel

@Composable
@Preview
fun App() {
    val viewModel = remember { ChatViewModel() }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ChatScreen(viewModel)
        }
    }
}